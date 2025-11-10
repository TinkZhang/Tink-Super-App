package app.tinks.tink.weight

import app.tinks.tink.weight.db.WeightDao
import app.tinks.tink.weight.db.WeightEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

package app.tinks.tink.weight

import app.tinks.tink.weight.data.*
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
            val remoteList = table.select {
                order("created_at", order = Order.DESCENDING)
            }.decodeList<WeightRecord>()

            val entities = remoteList.map { it.toEntity() }
            dao.clearAll()
            entities.forEach { dao.insertWeight(it) }
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



@Serializable
data class WeightRecord(
    val id: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val weight: Double
)