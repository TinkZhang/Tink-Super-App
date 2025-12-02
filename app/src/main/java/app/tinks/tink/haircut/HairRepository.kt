package app.tinks.tink.haircut

import app.tinks.tink.haircut.db.HaircutDao
import app.tinks.tink.haircut.data.HairRecord
import app.tinks.tink.haircut.data.toEntity
import app.tinks.tink.haircut.db.HaircutEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Singleton
class HaircutRepository @Inject constructor(
    private val dao: HaircutDao,
    private val supabase: SupabaseClient
) {
    private val table = supabase.from("haircut")

    fun getAllHaircutsFlow(): Flow<List<HaircutEntity>> = dao.getAllHaircutsFlow()

    suspend fun addHaircut(price: Int, shopName: String) {
        val entity = HaircutEntity(
            price = price,
            shopName = shopName,
            date = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date,
            isSynced = false
        )
        dao.insertHaircut(entity)
        trySync(entity)
    }

    suspend fun deleteHaircut(id: Int) {
        dao.markDeleted(id)
        trySyncDeleted(id)
    }

    suspend fun refreshFromRemote() {
        withContext(Dispatchers.IO) {
            val remoteList = table.select {
                order("created_at", order = Order.DESCENDING)
            }.decodeList<HairRecord>()

            val entities = remoteList.map { it.toEntity() }
            dao.clearAll()
            entities.forEach { dao.insertHaircut(it) }
        }
    }

    suspend fun syncPending() {
        val unsynced = dao.getUnsyncedHaircuts()
        for (r in unsynced) trySync(r)
    }

    private suspend fun trySync(entity: HaircutEntity) {
        try {
            val record = entity
            table.insert(record)
            dao.markSynced(entity.localId)
        } catch (_: Exception) {
            // 离线时忽略
        }
    }

    private suspend fun trySyncDeleted(id: Int) {
        try {
            val item = dao.getAllHaircuts().find { it.localId == id } ?: return
            item.remoteId?.let {
                table.delete { filter { eq("id", it) } }
            }
            dao.deleteById(id)
        } catch (_: Exception) {
        }
    }
}
