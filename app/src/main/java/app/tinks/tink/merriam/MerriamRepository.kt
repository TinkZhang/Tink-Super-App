package app.tinks.tink.merriam

import app.tinks.tink.merriam.data.Stat
import app.tinks.tink.merriam.data.Unit as MerriamUnit
import app.tinks.tink.merriam.db.MerriamDao
import app.tinks.tink.merriam.db.RootEntity
import app.tinks.tink.merriam.db.toRoot
import app.tinks.tink.merriam.network.MerriamApi
import app.tinks.tink.merriam.network.RootPostDto
import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class MerriamRepository @Inject constructor(
    private val dao: MerriamDao,
    private val api: MerriamApi,
) {
    open fun getAllMerriamsFlow(): Flow<List<RootEntity>> = dao.getAllRootsFlow()

    open fun getAllUnitsFlow(): Flow<List<MerriamUnit>> = dao.getAllRootsFlow().map { roots ->
        roots.groupBy { it.unit }
            .map { (unitId, group) ->
                MerriamUnit(
                    id = unitId,
                    roots = group.map { it.toRoot() }
                )
            }
    }

    open fun addMerriamRecords(records: List<RootPostDto>): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { api.postMerriam(records) })
    }.flowOn(Dispatchers.IO)

    open fun getMerriamStat(): Flow<ApiResult<Stat>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall {
            api.getStat().toDomain()
        })
    }.flowOn(Dispatchers.IO)
}
