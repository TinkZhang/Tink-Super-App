package app.tinks.tink.salesforce

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.ui.theme.TinkTheme

@Composable
fun SalesforceScreen(viewModel: SalesforceViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SalesforceScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
internal fun SalesforceScreen(
    state: SalesforceUiState,
    onEvent: (SalesforceEvent) -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (state.mode) {
            SalesforceViewMode.Main -> SalesforceMainScreen(state, onEvent)
            SalesforceViewMode.Practice -> SalesforceQuestionSessionScreen(state, onEvent, SalesforceMode.Practice)
            SalesforceViewMode.Exam -> SalesforceQuestionSessionScreen(state, onEvent, SalesforceMode.Exam)
            SalesforceViewMode.ExamResult -> SalesforceExamResultScreen(state, onEvent)
            SalesforceViewMode.Review -> SalesforceReviewScreen(state, onEvent)
        }

        if (state.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .testTag("salesforce_loading_indicator")
            )
        }
    }
}

@Composable
private fun SalesforceMainScreen(
    state: SalesforceUiState,
    onEvent: (SalesforceEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("salesforce_main"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Salesforce ADM-201",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatTile("Questions", state.totalQuestions.toString())
                        StatTile("DONE", "${state.doneQuestions}/${state.totalQuestions}")
                        StatTile("Remaining", state.remainingQuestions.toString())
                        StatTile("Attempts", state.answeredAttempts.toString())
                        state.remoteSummary?.latestExamScore?.let { StatTile("Latest exam", "${it.cleanPercent()}%") }
                        state.remoteSummary?.bestExamScore?.let { StatTile("Best exam", "${it.cleanPercent()}%") }
                    }
                }
            }
        }

        item {
            ModeCard(
                title = "Practice",
                subtitle = "Review one question at a time, reveal the answer immediately, and sync when you return here.",
                icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                action = "Start",
                enabled = state.questions.isNotEmpty(),
                onClick = { onEvent(SalesforceEvent.StartPractice) },
                testTag = "salesforce_start_practice",
            )
        }

        item {
            ModeCard(
                title = "Exam",
                subtitle = "Take a random 65-question attempt. Answers stay hidden until the final result page.",
                icon = { Icon(Icons.Filled.Assignment, contentDescription = null) },
                action = "Start",
                enabled = state.questions.size >= SALESFORCE_EXAM_QUESTION_COUNT,
                onClick = { onEvent(SalesforceEvent.StartExam) },
                testTag = "salesforce_start_exam",
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.testTag("salesforce_stat_${label.lowercase().replace(" ", "_")}"),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    action: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    icon()
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                enabled = enabled,
                onClick = onClick,
            ) {
                Text(action)
            }
        }
    }
}

@Composable
private fun SalesforceQuestionSessionScreen(
    state: SalesforceUiState,
    onEvent: (SalesforceEvent) -> Unit,
    mode: SalesforceMode,
) {
    val question = state.currentQuestion
    if (question == null) {
        EmptySalesforceState(onEvent)
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(if (mode == SalesforceMode.Practice) "salesforce_practice" else "salesforce_exam"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            QuestionHeader(
                title = if (mode == SalesforceMode.Practice) "Practice" else "Exam",
                position = state.currentPosition,
                total = state.totalInMode,
                questionId = question.id,
            )
        }

        item {
            QuestionCard(
                question = question,
                selectedLabels = state.selectedLabels,
                revealAnswer = state.answerRevealed,
                onToggle = { onEvent(SalesforceEvent.ToggleAnswer(it)) },
            )
        }

        if (mode == SalesforceMode.Practice && state.answerRevealed) {
            item {
                AnswerPanel(
                    isCorrect = state.isCurrentAnswerCorrect,
                    explanation = question.explanation,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onEvent(SalesforceEvent.FinishPractice) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (mode == SalesforceMode.Practice) "Finish" else "Exit")
                }
                Button(
                    enabled = if (mode == SalesforceMode.Practice) {
                        state.canSubmit || state.answerRevealed
                    } else {
                        state.canSubmit
                    },
                    onClick = {
                        if (mode == SalesforceMode.Practice) {
                            if (state.answerRevealed) {
                                onEvent(SalesforceEvent.NextPracticeQuestion)
                            } else {
                                onEvent(SalesforceEvent.SubmitPracticeAnswer)
                            }
                        } else {
                            onEvent(SalesforceEvent.NextExamQuestion)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            mode == SalesforceMode.Exam && state.currentPosition == state.totalInMode -> "Finish exam"
                            mode == SalesforceMode.Exam -> "Next"
                            state.answerRevealed -> "Next"
                            else -> "Check"
                        }
                    )
                }
                if (mode == SalesforceMode.Practice) {
                    Button(
                        onClick = { onEvent(SalesforceEvent.MarkPracticeQuestionDone) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("salesforce_done_button"),
                    ) {
                        Text("DONE")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionHeader(
    title: String,
    position: Int,
    total: Int,
    questionId: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = "$position / $total", style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else position.toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(text = "NO. $questionId", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun QuestionCard(
    question: SalesforceQuestion,
    selectedLabels: Set<String>,
    revealAnswer: Boolean,
    onToggle: (String) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.titleMedium,
            )
            HorizontalDivider()
            question.choices.forEach { choice ->
                val isSelected = choice.label in selectedLabels
                val isCorrect = choice.label in question.answerLabels
                ChoiceRow(
                    choice = choice,
                    selected = isSelected,
                    revealAnswer = revealAnswer,
                    isCorrect = isCorrect,
                    onClick = { onToggle(choice.label) },
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    choice: SalesforceChoice,
    selected: Boolean,
    revealAnswer: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        revealAnswer && isCorrect -> MaterialTheme.colorScheme.primary
        revealAnswer && selected -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !revealAnswer, onClick = onClick)
            .testTag("salesforce_choice_${choice.label}"),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        color = when {
            revealAnswer && isCorrect -> MaterialTheme.colorScheme.primaryContainer
            revealAnswer && selected -> MaterialTheme.colorScheme.errorContainer
            selected -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            FilterChip(
                selected = selected,
                onClick = onClick,
                enabled = !revealAnswer,
                label = { Text(choice.label) },
            )
            Text(
                text = choice.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AnswerPanel(
    isCorrect: Boolean,
    explanation: String?,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("salesforce_answer_panel"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isCorrect) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Close,
                    contentDescription = null,
                )
                Text(
                    text = if (isCorrect) {
                        "Correct. Use DONE when you have mastered it."
                    } else {
                        "Not yet. Keep this one in rotation."
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = explanation?.takeIf { it.isNotBlank() } ?: "No explanation is included for this question.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SalesforceExamResultScreen(
    state: SalesforceUiState,
    onEvent: (SalesforceEvent) -> Unit,
) {
    val result = state.examResult ?: return EmptySalesforceState(onEvent)
    val questionById = result.questions.associateBy { it.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("salesforce_exam_result"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = "Exam result", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "${result.correctCount}/${result.totalCount} · ${result.scorePercent.cleanPercent()}%",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Button(onClick = { onEvent(SalesforceEvent.BackToMain) }) {
                        Text("Back to main")
                    }
                }
            }
        }

        items(result.events, key = { it.questionId }) { event ->
            val question = questionById[event.questionId] ?: return@items
            ResultRow(
                question = question,
                event = event,
                onClick = { onEvent(SalesforceEvent.ReviewExamQuestion(question.id)) },
            )
        }
    }
}

@Composable
private fun ResultRow(
    question: SalesforceQuestion,
    event: SalesforceAnswerRecord,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("salesforce_exam_result_${question.id}"),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (event.correct) "✅" else "❌",
                style = MaterialTheme.typography.titleMedium,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "NO. ${question.id}", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = question.prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun SalesforceReviewScreen(
    state: SalesforceUiState,
    onEvent: (SalesforceEvent) -> Unit,
) {
    val question = state.reviewQuestion ?: return EmptySalesforceState(onEvent)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("salesforce_exam_review"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            QuestionHeader(
                title = "Review",
                position = 1,
                total = 1,
                questionId = question.id,
            )
        }
        item {
            QuestionCard(
                question = question,
                selectedLabels = state.selectedLabels,
                revealAnswer = true,
                onToggle = {},
            )
        }
        item {
            AnswerPanel(
                isCorrect = state.selectedLabels.sorted() == question.answerLabels.sorted(),
                explanation = question.explanation,
            )
        }
        item {
            Button(
                onClick = { onEvent(SalesforceEvent.BackToExamResult) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Back to result")
            }
        }
    }
}

@Composable
private fun EmptySalesforceState(onEvent: (SalesforceEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Salesforce questions are not ready yet.")
        OutlinedButton(onClick = { onEvent(SalesforceEvent.BackToMain) }) {
            Text("Back")
        }
    }
}

private fun Double.cleanPercent(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format("%.2f", this)

@PreviewLightDark
@Composable
private fun SalesforceMainPreview() {
    TinkTheme {
        SalesforceScreen(
            state = SalesforceUiState(
                questions = previewQuestions,
                progress = mapOf(1 to SalesforceQuestionProgress(1, true, 1, 0, null)),
                remoteSummary = SalesforceProgressSummary(
                    totalQuestions = 153,
                    doneQuestions = 42,
                    practiceAttempts = 96,
                    examAttempts = 2,
                    latestExamScore = 72.31,
                    bestExamScore = 81.54,
                )
            )
        )
    }
}

private val previewQuestions = listOf(
    SalesforceQuestion(
        id = 1,
        prompt = "Which setup option should an administrator use for a private discussion space?",
        answerLabels = listOf("A"),
        explanation = "Use the option that hides the group from non-members while still allowing members to collaborate.",
        choices = listOf(
            SalesforceChoice("A", "Chatter Unlisted Group"),
            SalesforceChoice("B", "Chatter Public Group"),
            SalesforceChoice("C", "Chatter Private Group"),
            SalesforceChoice("D", "Private Chatter Channel"),
        )
    )
)
