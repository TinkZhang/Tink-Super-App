package app.tinks.tink.merriam

import app.tinks.tink.merriam.db.MerriamDao
import app.tinks.tink.merriam.db.RootEntity
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerriamRepository @Inject constructor(
    private val dao: MerriamDao,
    private val supabase: SupabaseClient
) {
//    private val table = supabase.from("Zi")
//
    fun getAllMerriamsFlow(): Flow<List<RootEntity>> = dao.getAllRootsFlow()
//
//    suspend fun addZi(Zi: Double) {
//        val entity = ZiEntity(
//            Zi = Zi,
//            createdAt = System.currentTimeMillis(),
//            isSynced = false
//        )
//        dao.insertZi(entity)
//        trySync(entity)
//    }
//
//    suspend fun deleteZi(id: Int) {
//        dao.markDeleted(id)
//        trySyncDeleted(id)
//    }
//
//    suspend fun updateZi(id: Int, Zi: Double) {
//        val list = dao.getAllZis()
//        val target = list.find { it.localId == id } ?: return
//        val updated = target.copy(Zi = Zi, isSynced = false)
//        dao.updateZi(updated)
//        trySync(updated)
//    }
//
//    suspend fun refreshFromRemote() {
//        withContext(Dispatchers.IO) {
//            val remoteList = table.select {
//                order("created_at", order = Order.DESCENDING)
//            }.decodeList<ZiRecord>()
//
//            val entities = remoteList.map { it.toEntity() }
//            dao.clearAll()
//            entities.forEach { dao.insertZi(it) }
//        }
//    }
//
//    suspend fun syncPending() {
//        val unsynced = dao.getUnsyncedZis()
//        for (r in unsynced) trySync(r)
//    }
//
//    private suspend fun trySync(entity: ZiEntity) {
//        try {
//            val record = entity.toRecord()
//            table.insert(record)
//            dao.markSynced(entity.localId)
//        } catch (_: Exception) {
//            // 离线时忽略
//        }
//    }
//
//    private suspend fun trySyncDeleted(id: Int) {
//        try {
//            val item = dao.getAllZis().find { it.localId == id } ?: return
//            item.remoteId?.let {
//                table.delete { filter { eq("id", it) } }
//            }
//            dao.deleteById(id)
//        } catch (_: Exception) { }
//    }
}
