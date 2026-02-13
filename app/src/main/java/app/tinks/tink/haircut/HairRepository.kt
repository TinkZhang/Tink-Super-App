package app.tinks.tink.haircut

import android.util.Log
import app.tinks.tink.haircut.data.HairRecord
import app.tinks.tink.haircut.data.toEntity
import app.tinks.tink.haircut.data.toRecord
import app.tinks.tink.haircut.db.HaircutDao
import app.tinks.tink.haircut.db.HaircutEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
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

    suspend fun addHaircut(
        price: Int,
        shopName: String,
        date: LocalDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    ) {
        val entity = HaircutEntity(
            price = price,
            shopName = shopName,
            date = date,
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

            val remoteEntities = remoteList.map { it.toEntity() }

            // Get all local entities (including unsynced ones)
            val localEntities = dao.getAllHaircuts()

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
                        dao.updateHaircut(remoteEntity)
                    }
                } else {
                    // Remote entity doesn't exist locally, insert it
                    dao.insertHaircut(remoteEntity)
                }
            }

            // Handle deletions - remove local records that exist in DB but not in remote data
            // (but only if they were synced and not modified locally)
            for (localEntity in localEntities) {
                if (localEntity.remoteId != null && !remoteEntityMap.containsKey(localEntity.remoteId)) {
                    // This remote record doesn't exist anymore, but only delete if it wasn't modified locally
                    if (localEntity.isSynced) {
                        localEntity.localId?.let { dao.deleteById(it) }
                    }
                }
            }
        }
    }

    suspend fun syncPending() {
        val unsynced = dao.getUnsyncedHaircuts()
        for (r in unsynced) trySync(r)
    }

    private suspend fun trySync(entity: HaircutEntity) {
        try {
            table.insert(entity.toRecord())
            dao.markSynced(entity.localId)
        } catch (e: Exception) {
            Log.d("Supabase", "$e")
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
