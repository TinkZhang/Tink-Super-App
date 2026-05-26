package app.tinks.tink.haircut

import app.tinks.tink.haircut.data.Haircut
import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class HaircutRepository @Inject constructor(
    private val api: HaircutApi,
) {
    open fun getHaircuts(): Flow<ApiResult<List<Haircut>>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.getHaircuts()
                    .map { it.toDomain() }
                    .sortedByDescending { it.date }
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun addHaircut(
        price: Int,
        shopName: String,
        date: LocalDate,
    ): Flow<ApiResult<Haircut>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.createHaircut(
                    payload = HaircutCreateRequest(
                        shopName = shopName,
                        price = price,
                        createdAt = date,
                    )
                ).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun deleteHaircut(id: Int): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                api.deleteHaircut(haircutId = id)
            }
        )
    }.flowOn(Dispatchers.IO)
}
