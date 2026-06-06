package app.tinks.tink.salesforce

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SalesforceApi {
    @GET("salesforce/progress")
    suspend fun getProgress(): SalesforceProgressDto

    @POST("salesforce/practice-sessions")
    suspend fun postPracticeSession(
        @Body payload: SalesforcePracticeSessionRequest,
    ): SalesforcePracticeSessionResponseDto

    @POST("salesforce/exam-attempts")
    suspend fun postExamAttempt(
        @Body payload: SalesforceExamAttemptRequest,
    ): SalesforceExamAttemptDto
}

@Serializable
data class SalesforceAnswerEventRequest(
    @SerialName("question_id")
    val questionId: Int,
    val mode: String,
    @SerialName("selected_labels")
    val selectedLabels: List<String>,
    @SerialName("correct_labels")
    val correctLabels: List<String>,
    val correct: Boolean,
    @SerialName("answered_at")
    val answeredAt: String,
)

@Serializable
data class SalesforcePracticeSessionRequest(
    val events: List<SalesforceAnswerEventRequest>,
)

@Serializable
data class SalesforcePracticeSessionResponseDto(
    @SerialName("accepted_events")
    val acceptedEvents: Int,
    val progress: SalesforceProgressDto,
)

@Serializable
data class SalesforceExamAttemptRequest(
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("finished_at")
    val finishedAt: String,
    val events: List<SalesforceAnswerEventRequest>,
)

@Serializable
data class SalesforceExamAttemptDto(
    val id: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("finished_at")
    val finishedAt: String,
    @SerialName("total_count")
    val totalCount: Int,
    @SerialName("correct_count")
    val correctCount: Int,
    @SerialName("score_percent")
    val scorePercent: Double,
    val results: List<SalesforceAnswerEventRequest>,
)

@Serializable
data class SalesforceQuestionProgressDto(
    @SerialName("question_id")
    val questionId: Int,
    val done: Boolean,
    @SerialName("correct_count")
    val correctCount: Int,
    @SerialName("incorrect_count")
    val incorrectCount: Int,
    @SerialName("last_answered_at")
    val lastAnsweredAt: String? = null,
)

@Serializable
data class SalesforceProgressDto(
    @SerialName("total_questions")
    val totalQuestions: Int,
    @SerialName("done_questions")
    val doneQuestions: Int,
    @SerialName("practice_attempts")
    val practiceAttempts: Int,
    @SerialName("exam_attempts")
    val examAttempts: Int,
    @SerialName("latest_exam_score")
    val latestExamScore: Double? = null,
    @SerialName("best_exam_score")
    val bestExamScore: Double? = null,
    val questions: List<SalesforceQuestionProgressDto>,
)

fun SalesforceAnswerRecord.toRequest(): SalesforceAnswerEventRequest =
    SalesforceAnswerEventRequest(
        questionId = questionId,
        mode = mode.wireValue,
        selectedLabels = selectedLabels.sorted(),
        correctLabels = correctLabels.sorted(),
        correct = correct,
        answeredAt = answeredAt,
    )

fun SalesforceQuestionProgressDto.toEntity(): SalesforceLocalProgressEntity =
    SalesforceLocalProgressEntity(
        questionId = questionId,
        done = done,
        correctCount = correctCount,
        incorrectCount = incorrectCount,
        lastAnsweredAt = lastAnsweredAt,
    )

fun SalesforceProgressDto.toSummary(): SalesforceProgressSummary =
    SalesforceProgressSummary(
        totalQuestions = totalQuestions,
        doneQuestions = doneQuestions,
        practiceAttempts = practiceAttempts,
        examAttempts = examAttempts,
        latestExamScore = latestExamScore,
        bestExamScore = bestExamScore,
    )
