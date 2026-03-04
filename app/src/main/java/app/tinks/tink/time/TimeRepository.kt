package app.tinks.tink.time

import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeRepository @Inject constructor(
    private val api: TimeApi,
) {
    fun getTimeDashboard(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<ApiResult<TimeDashboard>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                coroutineScope {
                    val start = startDate.toString()
                    val end = endDate.toString()
                    val statisticsDeferred = async {
                        api.getStatistics(startDate = start, endDate = end)
                            .map { it.toDomain() }
                    }
                    val entriesDeferred = async {
                        api.getTimeEntries(startDate = start, endDate = end)
                            .map { it.toDomain() }
                    }
                    TimeDashboard(
                        statistics = statisticsDeferred.await(),
                        entries = entriesDeferred.await(),
                    )
                }
            }
        )
    }.flowOn(Dispatchers.IO)

    fun createTimeEntry(payload: TimeUpsertRequest): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.createTimeEntry(payload = payload)
            }
        )
    }.flowOn(Dispatchers.IO)

    fun updateTimeEntry(timeId: Long, payload: TimeUpsertRequest): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.updateTimeEntry(timeId = timeId, payload = payload)
            }
        )
    }.flowOn(Dispatchers.IO)

    fun deleteTimeEntry(timeId: Long): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.deleteTimeEntry(timeId = timeId)
            }
        )
    }.flowOn(Dispatchers.IO)
}
