package app.tinks.tink.weight

import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import app.tinks.tink.weight.data.Weight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class WeightRepository @Inject constructor(
    private val api: WeightApi,
) {
    open fun getWeights(): Flow<ApiResult<List<Weight>>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.getWeights()
                    .map { it.toDomain() }
                    .sortedByDescending { it.createdTime }
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun addWeight(weight: Double): Flow<ApiResult<Weight>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.createWeight(payload = WeightCreateRequest(weight = weight)).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun deleteWeight(id: Int): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.deleteWeight(weightId = id)
            }
        )
    }.flowOn(Dispatchers.IO)
}
