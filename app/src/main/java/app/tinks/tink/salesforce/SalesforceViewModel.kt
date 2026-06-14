package app.tinks.tink.salesforce

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.network.ApiResult
import app.tinks.tink.ui.components.AppSnackbarBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

sealed interface SalesforceEvent {
    object Refresh : SalesforceEvent
    object StartPractice : SalesforceEvent
    object StartExam : SalesforceEvent
    object SubmitPracticeAnswer : SalesforceEvent
    object NextPracticeQuestion : SalesforceEvent
    object MarkPracticeQuestionDone : SalesforceEvent
    object FinishPractice : SalesforceEvent
    object NextExamQuestion : SalesforceEvent
    object BackToMain : SalesforceEvent
    object BackToExamResult : SalesforceEvent
    data class ToggleAnswer(val label: String) : SalesforceEvent
    data class ReviewExamQuestion(val questionId: Int) : SalesforceEvent
}

enum class SalesforceViewMode {
    Main,
    Practice,
    Exam,
    ExamResult,
    Review,
}

data class SalesforceUiState(
    val mode: SalesforceViewMode = SalesforceViewMode.Main,
    val isLoading: Boolean = false,
    val questions: List<SalesforceQuestion> = emptyList(),
    val progress: Map<Int, SalesforceQuestionProgress> = emptyMap(),
    val remoteSummary: SalesforceProgressSummary? = null,
    val currentQuestion: SalesforceQuestion? = null,
    val currentPosition: Int = 0,
    val totalInMode: Int = 0,
    val selectedLabels: Set<String> = emptySet(),
    val answerRevealed: Boolean = false,
    val practiceEvents: List<SalesforceAnswerRecord> = emptyList(),
    val practiceDoneQuestionIds: Set<Int> = emptySet(),
    val examQuestions: List<SalesforceQuestion> = emptyList(),
    val examEvents: List<SalesforceAnswerRecord> = emptyList(),
    val examStartedAt: String? = null,
    val examResult: SalesforceExamResult? = null,
    val reviewQuestion: SalesforceQuestion? = null,
) {
    val totalQuestions: Int get() = questions.size
    val doneQuestions: Int get() = progress.values.count { it.done }
    val remainingQuestions: Int get() = (totalQuestions - doneQuestions).coerceAtLeast(0)
    val answeredAttempts: Int get() = progress.values.sumOf { it.correctCount + it.incorrectCount }
    val canSubmit: Boolean get() = selectedLabels.isNotEmpty()
    val isCurrentAnswerCorrect: Boolean
        get() = currentQuestion?.let { selectedLabels.sorted() == it.answerLabels.sorted() } == true
}

@HiltViewModel
class SalesforceViewModel @Inject constructor(
    private val repository: SalesforceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SalesforceUiState())
    val uiState = _state
        .stateIn(viewModelScope, SharingStarted.Eagerly, SalesforceUiState())

    private var syncJob: Job? = null

    init {
        combine(
            repository.observeQuestions(),
            repository.observeLocalProgress(),
        ) { questions, progress ->
            questions to progress
        }.onEach { (questions, progress) ->
            _state.update { state ->
                state.copy(
                    questions = questions,
                    progress = progress,
                    currentQuestion = state.currentQuestion?.let { current ->
                        questions.firstOrNull { it.id == current.id }
                    },
                    examQuestions = state.examQuestions.mapNotNull { examQuestion ->
                        questions.firstOrNull { it.id == examQuestion.id }
                    }.ifEmpty { state.examQuestions },
                    reviewQuestion = state.reviewQuestion?.let { review ->
                        questions.firstOrNull { it.id == review.id }
                    },
                )
            }
        }.launchIn(viewModelScope)

        syncProgress()
    }

    fun onEvent(event: SalesforceEvent) {
        when (event) {
            SalesforceEvent.Refresh -> syncProgress()
            SalesforceEvent.StartPractice -> startPractice()
            SalesforceEvent.StartExam -> startExam()
            is SalesforceEvent.ToggleAnswer -> toggleAnswer(event.label)
            SalesforceEvent.SubmitPracticeAnswer -> submitPracticeAnswer()
            SalesforceEvent.NextPracticeQuestion -> moveToNextPracticeQuestion()
            SalesforceEvent.MarkPracticeQuestionDone -> markPracticeQuestionDone()
            SalesforceEvent.FinishPractice -> finishPractice()
            SalesforceEvent.NextExamQuestion -> nextExamQuestion()
            SalesforceEvent.BackToMain -> finishPractice()
            is SalesforceEvent.ReviewExamQuestion -> reviewExamQuestion(event.questionId)
            SalesforceEvent.BackToExamResult -> _state.update {
                it.copy(mode = SalesforceViewMode.ExamResult, reviewQuestion = null)
            }
        }
    }

    private fun syncProgress() {
        syncJob?.cancel()
        syncJob = repository.syncRemoteProgress()
            .onEach(::handleSummaryResult)
            .launchIn(viewModelScope)
    }

    private fun startPractice() {
        val state = _state.value
        val firstQuestion = state.questions.firstOrNull { state.progress[it.id]?.done != true }
            ?: state.questions.firstOrNull()
            ?: return

        _state.update {
            it.copy(
                mode = SalesforceViewMode.Practice,
                currentQuestion = firstQuestion,
                currentPosition = firstQuestion.practicePosition(it.questions),
                totalInMode = it.questions.size,
                selectedLabels = emptySet(),
                answerRevealed = false,
                practiceEvents = emptyList(),
                practiceDoneQuestionIds = emptySet(),
            )
        }
    }

    private fun startExam() {
        val questions = _state.value.questions
        if (questions.size < SALESFORCE_EXAM_QUESTION_COUNT) return
        val examQuestions = questions.shuffled().take(SALESFORCE_EXAM_QUESTION_COUNT)
        _state.update {
            it.copy(
                mode = SalesforceViewMode.Exam,
                examQuestions = examQuestions,
                examEvents = emptyList(),
                examStartedAt = Instant.now().toString(),
                currentQuestion = examQuestions.first(),
                currentPosition = 1,
                totalInMode = SALESFORCE_EXAM_QUESTION_COUNT,
                selectedLabels = emptySet(),
                answerRevealed = false,
                examResult = null,
            )
        }
    }

    private fun toggleAnswer(label: String) {
        if (_state.value.answerRevealed) return
        _state.update { state ->
            val nextSelection = if (label in state.selectedLabels) {
                state.selectedLabels - label
            } else {
                state.selectedLabels + label
            }
            state.copy(selectedLabels = nextSelection)
        }
    }

    private fun submitPracticeAnswer() {
        val state = _state.value
        val question = state.currentQuestion ?: return
        if (!state.canSubmit || state.answerRevealed) return

        val event = answerEvent(question, SalesforceMode.Practice, state.selectedLabels)
        viewModelScope.launch {
            repository.recordLocalAnswer(event)
            _state.update {
                it.copy(
                    answerRevealed = true,
                    practiceEvents = it.practiceEvents + event,
                )
            }
        }
    }

    private fun moveToNextPracticeQuestion() {
        val state = _state.value
        val current = state.currentQuestion ?: return
        val nextQuestion = state.nextPracticeQuestionAfter(current.id)
            ?: return

        _state.update {
            it.copy(
                currentQuestion = nextQuestion,
                currentPosition = nextQuestion.practicePosition(it.questions),
                selectedLabels = emptySet(),
                answerRevealed = false,
            )
        }
    }

    private fun markPracticeQuestionDone() {
        val questionId = _state.value.currentQuestion?.id ?: return
        viewModelScope.launch {
            repository.markLocalDone(questionId)
            _state.update { state ->
                state.copy(practiceDoneQuestionIds = state.practiceDoneQuestionIds + questionId)
            }
            val nextQuestion = _state.value.nextPracticeQuestionAfter(questionId, extraDoneQuestionIds = setOf(questionId))
            if (nextQuestion == null) {
                finishPractice()
            } else {
                _state.update {
                    it.copy(
                        currentQuestion = nextQuestion,
                        currentPosition = nextQuestion.practicePosition(it.questions),
                        selectedLabels = emptySet(),
                        answerRevealed = false,
                    )
                }
            }
        }
    }

    private fun finishPractice() {
        val events = _state.value.practiceEvents
        val doneQuestionIds = _state.value.practiceDoneQuestionIds.toList()
        _state.update {
            it.copy(
                mode = SalesforceViewMode.Main,
                currentQuestion = null,
                selectedLabels = emptySet(),
                answerRevealed = false,
                practiceEvents = emptyList(),
                practiceDoneQuestionIds = emptySet(),
            )
        }
        if (events.isEmpty() && doneQuestionIds.isEmpty()) return
        repository.postPracticeSession(events, doneQuestionIds)
            .onEach(::handleSummaryResult)
            .launchIn(viewModelScope)
    }

    private fun nextExamQuestion() {
        val state = _state.value
        val question = state.currentQuestion ?: return
        if (!state.canSubmit) return

        val event = answerEvent(question, SalesforceMode.Exam, state.selectedLabels)
        viewModelScope.launch {
            repository.recordLocalAnswer(event)
        }

        val events = state.examEvents + event
        val nextIndex = events.size
        if (nextIndex >= state.examQuestions.size) {
            finishExam(events)
        } else {
            _state.update {
                it.copy(
                    examEvents = events,
                    currentQuestion = it.examQuestions[nextIndex],
                    currentPosition = nextIndex + 1,
                    selectedLabels = emptySet(),
                )
            }
        }
    }

    private fun finishExam(events: List<SalesforceAnswerRecord>) {
        val state = _state.value
        val finishedAt = Instant.now().toString()
        val result = SalesforceExamResult(
            startedAt = state.examStartedAt,
            finishedAt = finishedAt,
            questions = state.examQuestions,
            events = events,
        )
        _state.update {
            it.copy(
                mode = SalesforceViewMode.ExamResult,
                examEvents = events,
                examResult = result,
                currentQuestion = null,
                selectedLabels = emptySet(),
            )
        }
        repository.postExamAttempt(
            startedAt = result.startedAt,
            finishedAt = result.finishedAt,
            events = events,
        ).onEach(::handleSummaryResult)
            .launchIn(viewModelScope)
    }

    private fun reviewExamQuestion(questionId: Int) {
        val question = _state.value.examResult?.questions?.firstOrNull { it.id == questionId } ?: return
        val event = _state.value.examResult?.events?.firstOrNull { it.questionId == questionId }
        _state.update {
            it.copy(
                mode = SalesforceViewMode.Review,
                reviewQuestion = question,
                selectedLabels = event?.selectedLabels?.toSet() ?: emptySet(),
                answerRevealed = true,
            )
        }
    }

    private fun answerEvent(
        question: SalesforceQuestion,
        mode: SalesforceMode,
        selectedLabels: Set<String>,
    ): SalesforceAnswerRecord {
        val selected = selectedLabels.sorted()
        val correct = selected == question.answerLabels.sorted()
        return SalesforceAnswerRecord(
            questionId = question.id,
            mode = mode,
            selectedLabels = selected,
            correctLabels = question.answerLabels.sorted(),
            correct = correct,
            answeredAt = Instant.now().toString(),
        )
    }

    private fun handleSummaryResult(result: ApiResult<SalesforceProgressSummary>) {
        when (result) {
            ApiResult.Loading -> _state.update { it.copy(isLoading = true) }
            is ApiResult.Success -> _state.update {
                it.copy(isLoading = false, remoteSummary = result.data)
            }
            is ApiResult.Error -> _state.update {
                it.copy(isLoading = false)
            }.also {
                AppSnackbarBus.showApiFailure(onRetry = ::syncProgress)
            }
        }
    }
}

private fun SalesforceQuestion.practicePosition(questions: List<SalesforceQuestion>): Int =
    questions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.plus(1) ?: id

private fun SalesforceUiState.nextPracticeQuestionAfter(
    currentQuestionId: Int,
    extraDoneQuestionIds: Set<Int> = emptySet(),
): SalesforceQuestion? {
    fun isPracticeCandidate(question: SalesforceQuestion): Boolean =
        question.id != currentQuestionId &&
            progress[question.id]?.done != true &&
            question.id !in extraDoneQuestionIds

    return questions
        .filter { it.id > currentQuestionId }
        .firstOrNull(::isPracticeCandidate)
        ?: questions.firstOrNull(::isPracticeCandidate)
}
