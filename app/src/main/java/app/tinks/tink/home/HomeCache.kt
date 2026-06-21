package app.tinks.tink.home

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "home_snapshot")
data class HomeSnapshotEntity(
    @PrimaryKey val id: Int = HOME_SNAPSHOT_ID,
    val merriamLatest: Int? = null,
    val readKeeperBookId: Long? = null,
    val readKeeperTitle: String? = null,
    val readKeeperCoverUrl: String? = null,
    val readKeeperPageFormat: String? = null,
    val readKeeperCurrentPage: Int? = null,
    val readKeeperProgressPercentage: Double? = null,
    val readKeeperPages: Int? = null,
    val readKeeperSessionStartedAt: Long? = null,
    val readKeeperSessionStartPage: Int? = null,
    val readKeeperSessionStartProgressPercentage: Double? = null,
    val haircutDays: Int? = null,
    val weightValue: Double? = null,
    val weightRecordedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "home_pending_action")
data class HomePendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val startIntValue: Int? = null,
    val targetIntValue: Int? = null,
    val bookId: Long? = null,
    val pageValue: Int? = null,
    val progressValue: Double? = null,
    val weightValue: Double? = null,
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
    val textValue: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface HomeDao {
    @Query("SELECT * FROM home_snapshot WHERE id = :id")
    fun observeSnapshot(id: Int = HOME_SNAPSHOT_ID): Flow<HomeSnapshotEntity?>

    @Query("SELECT * FROM home_snapshot WHERE id = :id")
    suspend fun getSnapshot(id: Int = HOME_SNAPSHOT_ID): HomeSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: HomeSnapshotEntity)

    @Insert
    suspend fun insertPendingAction(action: HomePendingActionEntity): Long

    @Query("SELECT * FROM home_pending_action ORDER BY createdAt ASC")
    suspend fun getPendingActions(): List<HomePendingActionEntity>

    @Query("DELETE FROM home_pending_action WHERE id = :id")
    suspend fun deletePendingAction(id: Long)
}

const val HOME_SNAPSHOT_ID = 0

internal const val HOME_ACTION_MERRIAM_COMPLETE = "merriam_complete"
internal const val HOME_ACTION_WEIGHT_ADD = "weight_add"
internal const val HOME_ACTION_BOOK_PROGRESS = "book_progress"
internal const val HOME_ACTION_BOOK_SESSION = "book_session"
