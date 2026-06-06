package app.tinks.tink.salesforce

import android.content.Context
import app.tinks.tink.di.NetworkModule
import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SalesforceRepository @Inject constructor(
    private val dao: SalesforceDao,
    private val api: SalesforceApi,
    @ApplicationContext private val context: Context? = null,
) {
    open fun observeQuestions(): Flow<List<SalesforceQuestion>> =
        dao.observeQuestions()
            .onStart { seedQuestionBankIfEmpty() }
            .map { questions -> questions.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    open fun observeLocalProgress(): Flow<Map<Int, SalesforceQuestionProgress>> =
        dao.observeProgress()
            .map { rows -> rows.associate { it.questionId to it.toDomain() } }
            .flowOn(Dispatchers.IO)

    open suspend fun recordLocalAnswer(event: SalesforceAnswerRecord) {
        val current = dao.getProgress(event.questionId)
        dao.upsertProgress(
            SalesforceLocalProgressEntity(
                questionId = event.questionId,
                done = (current?.done == true) || event.correct,
                correctCount = (current?.correctCount ?: 0) + if (event.correct) 1 else 0,
                incorrectCount = (current?.incorrectCount ?: 0) + if (event.correct) 0 else 1,
                lastAnsweredAt = event.answeredAt,
            )
        )
    }

    open fun syncRemoteProgress(): Flow<ApiResult<SalesforceProgressSummary>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.getProgress().also { progress ->
                    dao.upsertProgress(progress.questions.map { it.toEntity() })
                }.toSummary()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun postPracticeSession(events: List<SalesforceAnswerRecord>): Flow<ApiResult<SalesforceProgressSummary>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.postPracticeSession(SalesforcePracticeSessionRequest(events.map { it.toRequest() }))
                    .progress
                    .also { progress -> dao.upsertProgress(progress.questions.map { it.toEntity() }) }
                    .toSummary()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun postExamAttempt(
        startedAt: String?,
        finishedAt: String,
        events: List<SalesforceAnswerRecord>,
    ): Flow<ApiResult<SalesforceProgressSummary>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.postExamAttempt(
                    SalesforceExamAttemptRequest(
                        startedAt = startedAt,
                        finishedAt = finishedAt,
                        events = events.map { it.toRequest() },
                    )
                )
                api.getProgress().also { progress ->
                    dao.upsertProgress(progress.questions.map { it.toEntity() })
                }.toSummary()
            }
        )
    }.flowOn(Dispatchers.IO)

    private suspend fun seedQuestionBankIfEmpty() {
        val appContext = context ?: return
        if (dao.questionCount() > 0) return
        val asset = appContext.assets.open("database/salesforce_questions.json")
            .bufferedReader()
            .use { reader ->
                NetworkModule.json.decodeFromString<SalesforceQuestionBankAsset>(reader.readText())
            }
        val questions = asset.questions.map { question ->
            SalesforceQuestionEntity(
                id = question.id,
                prompt = question.prompt,
                answerLabels = question.answerLabels.sorted().joinToString(","),
                explanation = question.explanation,
            )
        }
        val choices = asset.questions.flatMap { question ->
            question.choices.map { choice ->
                SalesforceChoiceEntity(
                    questionId = question.id,
                    label = choice.label,
                    text = choice.text,
                )
            }
        }
        dao.replaceQuestionBank(questions, choices)
    }
}
