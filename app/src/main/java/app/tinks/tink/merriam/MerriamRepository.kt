package app.tinks.tink.merriam

import app.tinks.tink.merriam.data.Stat
import app.tinks.tink.merriam.db.MerriamDao
import app.tinks.tink.merriam.db.RootEntity
import app.tinks.tink.merriam.network.MerriamApi
import app.tinks.tink.merriam.network.RootPostDto
import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerriamRepository @Inject constructor(
    private val dao: MerriamDao,
    private val api: MerriamApi,
) {
    fun getAllMerriamsFlow(): Flow<List<RootEntity>> = dao.getAllRootsFlow()
    fun addMerriamRecord(id: Int, root: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { api.postMerriam(RootPostDto(rootId = id, root = root)) })
    }.flowOn(Dispatchers.IO)

    fun getMerriamStat(): Flow<ApiResult<Stat>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall {
            try {

                val result = api.getStat()
                result.toDomain()
            } catch (e: Exception) {
                println("API call failed with exception: ${e.message}")
                println("Exception stack trace: $e")
                throw e
            }
        })
    }.flowOn(Dispatchers.IO)
}
