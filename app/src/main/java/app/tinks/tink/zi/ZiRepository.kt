package app.tinks.tink.zi

import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZiRepository @Inject constructor(
    private val api: ZiApi,
) {
    private val allZis = MutableStateFlow<List<Zi>>(emptyList())

    fun getAllLearntZisFlow(): Flow<List<Zi>> = allZis.asStateFlow().map { zis ->
        zis.filter { it.proficiency >= 5 }
    }

    fun putZi(zi: String, proficiency: Int) = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall {
            try {
                api.putZi(
                    request = PutZiRequest(zi, proficiency)
                )
            } catch (e: Exception) {
                println("API call failed with exception: ${e.message}")
                println("Exception stack trace: $e")
                throw e
            }
        })
    }.flowOn(Dispatchers.IO)

    fun getAllZis(): Flow<ApiResult<List<Zi>>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall {
            try {
                api.getZis().map { it.toDomain() }
            } catch (e: Exception) {
                println("API call failed with exception: ${e.message}")
                println("Exception stack trace: $e")
                throw e
            }
        })
    }.flowOn(Dispatchers.IO)
}
