package app.tinks.tink.zi

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZiRepository @Inject constructor(
    private val dao: ZiDao,
    supabase: SupabaseClient
) {
    private val table = supabase.from("zi")

    fun getAllZisFlow(): Flow<List<ZiEntity>> = dao.getAllZisFlow()
    fun getAllLearntZisFlow(): Flow<List<ZiEntity>> = dao.getAllLearntZisFlow()

    suspend fun addZi(zi: String, proficiency: Int, date: LocalDate) {
        val entity = ZiEntity(
            zi = zi,
            lastDate = date,
            isSynced = false,
            proficiency = proficiency,
        )
        dao.insertZi(entity)
        trySync(entity)
    }

    suspend fun deleteZi(id: Int) {
        dao.markDeleted(id)
        trySyncDeleted(id)
    }

    suspend fun updateZi(id: Int, zi: String) {
        val list = dao.getAllZis()
        val target = list.find { it.localId == id } ?: return
        val updated = target.copy(zi = zi, isSynced = false)
        dao.updateZi(updated)
        trySync(updated)
    }

    suspend fun refreshFromRemote() {
        withContext(Dispatchers.IO) {
            val remoteList = table.select {
            }.decodeList<ZiRecord>()

            val entities = remoteList.map { it.toEntity() }
            dao.clearAll()
            entities.forEach { dao.insertZi(it) }
        }
    }

    suspend fun syncPending() {
        val unsynced = dao.getUnsyncedZis()
        for (r in unsynced) trySync(r)
    }

    private suspend fun trySync(entity: ZiEntity) {
        try {
            val record = entity.toRecord()
            table.insert(record)
            dao.markSynced(entity.localId)
        } catch (_: Exception) {
            // 离线时忽略
        }
    }

    private suspend fun trySyncDeleted(id: Int) {
        try {
            val item = dao.getAllZis().find { it.localId == id } ?: return
            item.remoteId?.let {
                table.delete { filter { eq("id", it) } }
            }
            dao.deleteById(id)
        } catch (_: Exception) {
        }
    }
}
