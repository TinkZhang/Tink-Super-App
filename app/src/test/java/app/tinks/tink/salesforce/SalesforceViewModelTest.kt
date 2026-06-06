package app.tinks.tink.salesforce

import app.tinks.tink.network.ApiResult
import app.tinks.tink.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SalesforceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun practice_recordsLocalAnswerAndPostsOnlyWhenFinished() = runTest {
        val repository = FakeSalesforceRepository(questionCount = 2)
        val viewModel = SalesforceViewModel(repository)

        viewModel.onEvent(SalesforceEvent.StartPractice)
        viewModel.onEvent(SalesforceEvent.ToggleAnswer("A"))
        viewModel.onEvent(SalesforceEvent.SubmitPracticeAnswer)

        assertEquals(1, repository.localEvents.size)
        assertTrue(repository.practicePosts.isEmpty())
        assertTrue(viewModel.uiState.value.answerRevealed)

        viewModel.onEvent(SalesforceEvent.FinishPractice)

        assertEquals(SalesforceViewMode.Main, viewModel.uiState.value.mode)
        assertEquals(1, repository.practicePosts.single().size)
        assertTrue(repository.localEvents.single().correct)
    }

    @Test
    fun examPostsAttemptOnlyAfterSixtyFiveAnswers() = runTest {
        val repository = FakeSalesforceRepository(questionCount = 66)
        val viewModel = SalesforceViewModel(repository)

        viewModel.onEvent(SalesforceEvent.StartExam)

        repeat(SALESFORCE_EXAM_QUESTION_COUNT - 1) {
            viewModel.onEvent(SalesforceEvent.ToggleAnswer("A"))
            viewModel.onEvent(SalesforceEvent.NextExamQuestion)
            assertTrue(repository.examPosts.isEmpty())
        }

        viewModel.onEvent(SalesforceEvent.ToggleAnswer("A"))
        viewModel.onEvent(SalesforceEvent.NextExamQuestion)

        assertEquals(SalesforceViewMode.ExamResult, viewModel.uiState.value.mode)
        assertEquals(SALESFORCE_EXAM_QUESTION_COUNT, repository.examPosts.single().size)
        assertEquals(SALESFORCE_EXAM_QUESTION_COUNT, repository.localEvents.size)
        assertFalse(viewModel.uiState.value.examResult?.events.orEmpty().any { !it.correct })
    }

    private class FakeSalesforceRepository(questionCount: Int) : SalesforceRepository(NoopDao, NoopApi) {
        private val questions = MutableStateFlow((1..questionCount).map(::sampleQuestion))
        private val progress = MutableStateFlow<Map<Int, SalesforceQuestionProgress>>(emptyMap())

        val localEvents = mutableListOf<SalesforceAnswerRecord>()
        val practicePosts = mutableListOf<List<SalesforceAnswerRecord>>()
        val examPosts = mutableListOf<List<SalesforceAnswerRecord>>()

        override fun observeQuestions(): Flow<List<SalesforceQuestion>> = questions

        override fun observeLocalProgress(): Flow<Map<Int, SalesforceQuestionProgress>> = progress

        override suspend fun recordLocalAnswer(event: SalesforceAnswerRecord) {
            localEvents.add(event)
            val current = progress.value[event.questionId]
            progress.value = progress.value + (
                event.questionId to SalesforceQuestionProgress(
                    questionId = event.questionId,
                    done = (current?.done == true) || event.correct,
                    correctCount = (current?.correctCount ?: 0) + if (event.correct) 1 else 0,
                    incorrectCount = (current?.incorrectCount ?: 0) + if (event.correct) 0 else 1,
                    lastAnsweredAt = event.answeredAt,
                )
                )
        }

        override fun syncRemoteProgress(): Flow<ApiResult<SalesforceProgressSummary>> =
            flowOf(ApiResult.Loading, ApiResult.Success(SalesforceProgressSummary(totalQuestions = questions.value.size)))

        override fun postPracticeSession(events: List<SalesforceAnswerRecord>): Flow<ApiResult<SalesforceProgressSummary>> {
            practicePosts.add(events)
            return flowOf(ApiResult.Loading, ApiResult.Success(SalesforceProgressSummary(totalQuestions = questions.value.size)))
        }

        override fun postExamAttempt(
            startedAt: String?,
            finishedAt: String,
            events: List<SalesforceAnswerRecord>,
        ): Flow<ApiResult<SalesforceProgressSummary>> {
            examPosts.add(events)
            return flowOf(ApiResult.Loading, ApiResult.Success(SalesforceProgressSummary(totalQuestions = questions.value.size)))
        }
    }

    private object NoopDao : SalesforceDao {
        override fun observeQuestions(): Flow<List<SalesforceQuestionWithChoices>> = flowOf(emptyList())
        override suspend fun questionCount(): Int = 0
        override fun observeProgress(): Flow<List<SalesforceLocalProgressEntity>> = flowOf(emptyList())
        override suspend fun getProgress(questionId: Int): SalesforceLocalProgressEntity? = null
        override suspend fun upsertProgress(progress: SalesforceLocalProgressEntity) = Unit
        override suspend fun upsertProgress(progress: List<SalesforceLocalProgressEntity>) = Unit
        override suspend fun upsertQuestions(questions: List<SalesforceQuestionEntity>) = Unit
        override suspend fun insertChoices(choices: List<SalesforceChoiceEntity>) = Unit
        override suspend fun deleteAllChoices() = Unit
        override suspend fun deleteAllQuestions() = Unit
    }

    private object NoopApi : SalesforceApi {
        override suspend fun getProgress(): SalesforceProgressDto = error("not used")
        override suspend fun postPracticeSession(
            payload: SalesforcePracticeSessionRequest,
        ): SalesforcePracticeSessionResponseDto = error("not used")

        override suspend fun postExamAttempt(
            payload: SalesforceExamAttemptRequest,
        ): SalesforceExamAttemptDto = error("not used")
    }
}

private fun sampleQuestion(id: Int): SalesforceQuestion =
    SalesforceQuestion(
        id = id,
        prompt = "Question $id",
        answerLabels = listOf("A"),
        explanation = "Explanation $id",
        choices = listOf(
            SalesforceChoice("A", "Correct"),
            SalesforceChoice("B", "Incorrect"),
        ),
    )
