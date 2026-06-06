package app.tinks.tink.salesforce

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val SALESFORCE_EXAM_QUESTION_COUNT = 65

data class SalesforceQuestion(
    val id: Int,
    val prompt: String,
    val answerLabels: List<String>,
    val explanation: String?,
    val choices: List<SalesforceChoice>,
)

data class SalesforceChoice(
    val label: String,
    val text: String,
)

@Serializable
data class SalesforceQuestionBankAsset(
    val questions: List<SalesforceQuestionAsset>,
)

@Serializable
data class SalesforceQuestionAsset(
    val id: Int,
    val prompt: String,
    @SerialName("answer_labels")
    val answerLabels: List<String>,
    val explanation: String? = null,
    val choices: List<SalesforceChoiceAsset>,
)

@Serializable
data class SalesforceChoiceAsset(
    val label: String,
    val text: String,
)

data class SalesforceQuestionProgress(
    val questionId: Int,
    val done: Boolean,
    val correctCount: Int,
    val incorrectCount: Int,
    val lastAnsweredAt: String?,
)

data class SalesforceAnswerRecord(
    val questionId: Int,
    val mode: SalesforceMode,
    val selectedLabels: List<String>,
    val correctLabels: List<String>,
    val correct: Boolean,
    val answeredAt: String,
)

enum class SalesforceMode(val wireValue: String) {
    Practice("practice"),
    Exam("exam"),
}

data class SalesforceProgressSummary(
    val totalQuestions: Int = 0,
    val doneQuestions: Int = 0,
    val practiceAttempts: Int = 0,
    val examAttempts: Int = 0,
    val latestExamScore: Double? = null,
    val bestExamScore: Double? = null,
)

data class SalesforceExamResult(
    val startedAt: String?,
    val finishedAt: String,
    val questions: List<SalesforceQuestion>,
    val events: List<SalesforceAnswerRecord>,
) {
    val totalCount: Int = events.size
    val correctCount: Int = events.count { it.correct }
    val scorePercent: Double = if (totalCount == 0) {
        0.0
    } else {
        kotlin.math.round((correctCount.toDouble() / totalCount.toDouble()) * 10000.0) / 100.0
    }
}

@Entity(tableName = "salesforce_question")
data class SalesforceQuestionEntity(
    @PrimaryKey
    val id: Int,
    val prompt: String,
    @ColumnInfo(name = "answer_labels")
    val answerLabels: String,
    val explanation: String?,
)

@Entity(
    tableName = "salesforce_choice",
    foreignKeys = [
        ForeignKey(
            entity = SalesforceQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["question_id"]),
        Index(value = ["question_id", "label"], unique = true),
    ],
)
data class SalesforceChoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "question_id")
    val questionId: Int,
    val label: String,
    val text: String,
)

@Entity(tableName = "salesforce_local_progress")
data class SalesforceLocalProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: Int,
    @ColumnInfo(defaultValue = "0")
    val done: Boolean = false,
    @ColumnInfo(name = "correct_count", defaultValue = "0")
    val correctCount: Int = 0,
    @ColumnInfo(name = "incorrect_count", defaultValue = "0")
    val incorrectCount: Int = 0,
    @ColumnInfo(name = "last_answered_at")
    val lastAnsweredAt: String? = null,
)

data class SalesforceQuestionWithChoices(
    @Embedded val question: SalesforceQuestionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "question_id",
    )
    val choices: List<SalesforceChoiceEntity>,
)

fun SalesforceQuestionWithChoices.toDomain(): SalesforceQuestion =
    SalesforceQuestion(
        id = question.id,
        prompt = question.prompt,
        answerLabels = question.answerLabels
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() },
        explanation = question.explanation,
        choices = choices
            .sortedBy { it.label }
            .map { SalesforceChoice(label = it.label, text = it.text) },
    )

fun SalesforceLocalProgressEntity.toDomain(): SalesforceQuestionProgress =
    SalesforceQuestionProgress(
        questionId = questionId,
        done = done,
        correctCount = correctCount,
        incorrectCount = incorrectCount,
        lastAnsweredAt = lastAnsweredAt,
    )
