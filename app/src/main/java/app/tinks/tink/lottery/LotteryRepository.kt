package app.tinks.tink.lottery

import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class LotteryRepository @Inject constructor(
    private val api: LotteryApi,
) {
    open fun getLotteryHistory(): Flow<ApiResult<List<LotteryTicket>>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.getLotteryHistory()
                    .map { it.toDomain() }
                    .sortedWith(newestLotteryFirst)
                    .withWinningNumbers()
            }
        )
    }.flowOn(Dispatchers.IO)

    private suspend fun List<LotteryTicket>.withWinningNumbers(): List<LotteryTicket> {
        val resultsByIssue = mapNotNull { it.result }.associateBy { it.issueId }.toMutableMap()
        val missingIssues = filter { it.checked && it.issueId !in resultsByIssue }
            .map { it.issueId }
            .distinct()

        for (issueId in missingIssues) {
            val result = safeApiCall { api.getLotteryResult(issueId).toDomain() }
            currentCoroutineContext().ensureActive()
            if (result is ApiResult.Success) resultsByIssue[issueId] = result.data
        }
        return map { ticket ->
            if (ticket.checked && ticket.result == null) {
                ticket.copy(result = resultsByIssue[ticket.issueId])
            } else {
                ticket
            }
        }
    }

    open fun createLottery(request: LotteryCreateRequest): Flow<ApiResult<LotteryTicket>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { api.createLottery(request).toDomain() })
    }.flowOn(Dispatchers.IO)

    open fun updateLottery(id: Int, request: LotteryUpdateRequest): Flow<ApiResult<LotteryTicket>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { api.updateLottery(id, request).toDomain() })
    }.flowOn(Dispatchers.IO)

    open fun deleteLottery(id: Int): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { api.deleteLottery(id) })
    }.flowOn(Dispatchers.IO)

    open fun checkLottery(id: Int): Flow<ApiResult<LotteryCheckOutcome>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { api.checkLottery(id).toDomain() })
    }.flowOn(Dispatchers.IO)

    open fun getLotteryResult(issueId: String): Flow<ApiResult<LotteryResult>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { api.getLotteryResult(issueId).toDomain() })
    }.flowOn(Dispatchers.IO)
}
