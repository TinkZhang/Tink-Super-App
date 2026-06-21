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
open class TimeRepository @Inject constructor(
    private val api: TimeApi,
) {
    open fun getTimeDashboard(
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

    open fun createTimeEntry(payload: TimeUpsertRequest): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.createTimeEntry(payload = payload)
                Unit
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun updateTimeEntry(timeId: Long, payload: TimeUpsertRequest): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.updateTimeEntry(timeId = timeId, payload = payload)
                Unit
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun deleteTimeEntry(timeId: Long): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.deleteTimeEntry(timeId = timeId)
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun getTimeLabels(type: Int? = null): Flow<ApiResult<List<TimeLabel>>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.getTimeLabels(type = type)
                    .map { it.toDomain() }
                    .sortedWith(compareBy<TimeLabel> { it.type }.thenBy { it.sortOrder }.thenBy { it.name })
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun createTimeLabel(type: Int, name: String): Flow<ApiResult<TimeLabel>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.createTimeLabel(
                    TimeLabelCreateRequest(
                        type = type,
                        name = name.trim(),
                    )
                ).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun updateTimeLabel(labelId: Long, type: Int, name: String): Flow<ApiResult<TimeLabel>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.updateTimeLabel(
                    labelId = labelId,
                    payload = TimeLabelUpdateRequest(
                        type = type,
                        name = name.trim(),
                    ),
                ).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun deleteTimeLabel(labelId: Long): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.deleteTimeLabel(labelId = labelId)
            }
        )
    }.flowOn(Dispatchers.IO)
}
