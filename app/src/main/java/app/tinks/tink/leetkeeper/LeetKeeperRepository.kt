package app.tinks.tink.leetkeeper

import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class LeetKeeperRepository @Inject constructor(
    private val api: LeetKeeperApi,
) {
    open fun getSnapshot(): Flow<ApiResult<LeetKeeperSnapshot>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                LeetKeeperSnapshot(
                    publicPlans = api.getPublicPlans().map { it.toDomain() },
                    ongoingPlan = api.getOngoingPlan()?.toDomain(),
                )
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun getPublicPlanDetail(planId: Int): Flow<ApiResult<LeetKeeperPublicPlan>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.getPublicPlanDetail(planId).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun startPlan(
        publicPlanId: Int,
        language: LeetKeeperLanguage,
    ): Flow<ApiResult<LeetKeeperOngoingPlan>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.createOngoingPlan(
                    LeetKeeperStartPlanRequest(
                        publicPlanId = publicPlanId,
                        language = language.label,
                    )
                ).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun getProblemDetail(problemId: String): Flow<ApiResult<LeetKeeperProblemDetail>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.getProblemDetail(problemId).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun markProblemDone(
        plan: LeetKeeperOngoingPlan,
        problem: LeetKeeperProblemSummary,
        durationMinutes: Int,
        submission: String?,
    ): Flow<ApiResult<LeetKeeperOngoingPlan>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.createTransaction(
                    LeetKeeperCreateTransactionRequest(
                        language = plan.language.label,
                        problemId = problem.id,
                        problemLink = problem.link,
                        time = durationMinutes,
                        submission = submission?.takeIf { it.isNotBlank() },
                    )
                )
                val doneProblemId = problem.id.toIntOrNull()
                val updatedDones = if (doneProblemId == null) {
                    plan.dones
                } else {
                    (plan.dones + doneProblemId).distinct()
                }
                api.updateOngoingPlan(
                    planId = plan.id,
                    payload = LeetKeeperUpdateOngoingPlanRequest(
                        dones = updatedDones,
                    ),
                ).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)
}
