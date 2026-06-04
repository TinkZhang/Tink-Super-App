package app.tinks.tink.lottery

import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import kotlinx.coroutines.Dispatchers
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
                    .sortedWith(compareByDescending<LotteryTicket> { it.revealTime }.thenByDescending { it.id })
            }
        )
    }.flowOn(Dispatchers.IO)

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
