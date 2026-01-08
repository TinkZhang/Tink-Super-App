package app.tinks.tink.weight

import app.tinks.tink.weight.data.WeightRecord
import app.tinks.tink.weight.data.toEntity
import app.tinks.tink.weight.data.toRecord
import app.tinks.tink.weight.db.WeightDao
import app.tinks.tink.weight.db.WeightEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepository @Inject constructor(
    private val dao: WeightDao,
    private val supabase: SupabaseClient
) {
    private val table = supabase.from("weight")

    fun getAllWeightsFlow(): Flow<List<WeightEntity>> = dao.getAllWeightsFlow()

    suspend fun addWeight(weight: Double) {
        val entity = WeightEntity(
            weight = weight,
            createdAt = System.currentTimeMillis(),
            isSynced = false
        )
        dao.insertWeight(entity)
        trySync(entity)
    }

    suspend fun deleteWeight(id: Int) {
        dao.markDeleted(id)
        trySyncDeleted(id)
    }

    suspend fun updateWeight(id: Int, weight: Double) {
        val list = dao.getAllWeights()
        val target = list.find { it.localId == id } ?: return
        val updated = target.copy(weight = weight, isSynced = false)
        dao.updateWeight(updated)
        trySync(updated)
    }

    suspend fun refreshFromRemote() {
        withContext(Dispatchers.IO) {
            try {
                val remoteList = table.select {
                    order("created_at", order = Order.DESCENDING)
                }.decodeList<WeightRecord>()

                val remoteEntities = remoteList.map { it.toEntity() }
                
                // Get all local entities (including unsynced ones)
                val localEntities = dao.getAllWeights()
                
                // Create a map of remote entities by their remote ID for quick lookup
                val remoteEntityMap = remoteEntities.associateBy { it.remoteId }
                
                // Process each remote entity
                for (remoteEntity in remoteEntities) {
                    // Check if this remote entity exists locally
                    val localEntity = localEntities.find { it.remoteId == remoteEntity.remoteId }
                    
                    if (localEntity != null) {
                        // Remote entity exists locally, check if it's been modified locally
                        if (!localEntity.isSynced) {
                            // Local version exists and hasn't been synced yet, so keep the local version
                            // This preserves local changes that haven't been synced
                            continue
                        } else {
                            // Local version exists and is synced, update it with remote version
                            dao.updateWeight(remoteEntity)
                        }
                    } else {
                        // Remote entity doesn't exist locally, insert it
                        dao.insertWeight(remoteEntity)
                    }
                }
                
                // Handle deletions - remove local records that exist in DB but not in remote data
                // (but only if they were synced and not modified locally)
                for (localEntity in localEntities) {
                    if (localEntity.remoteId != null && !remoteEntityMap.containsKey(localEntity.remoteId)) {
                        // This remote record doesn't exist anymore, but only delete if it wasn't modified locally
                        if (localEntity.isSynced) {
                            dao.deleteById(localEntity.localId)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncPending() {
        val unsynced = dao.getUnsyncedWeights()
        for (r in unsynced) trySync(r)
    }

    private suspend fun trySync(entity: WeightEntity) {
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
            val item = dao.getAllWeights().find { it.localId == id } ?: return
            item.remoteId?.let {
                table.delete { filter { eq("id", it) } }
            }
            dao.deleteById(id)
        } catch (_: Exception) { }
    }
}