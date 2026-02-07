package app.tinks.tink.merriam.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MerriamDao {
    @Query("SELECT * FROM merriam ORDER BY id ASC")
    fun getAllRootsFlow(): Flow<List<RootEntity>>
    
    @Insert
    suspend fun insertMerriamRecord(record: MerriamRecordEntity)
    
    @Query("SELECT * FROM merriam_record WHERE root_id = :rootId")
    suspend fun getRecordsByRootId(rootId: Int): List<MerriamRecordEntity>
    
    @Query("SELECT r.*, m.root, m.words FROM merriam_record r JOIN merriam m ON r.root_id = m.id WHERE r.root_id = :rootId ORDER BY r.round DESC")
    suspend fun getRecordsWithRootInfoByRootId(rootId: Int): List<MerriamRecordWithRootInfo>
    
    @Query("SELECT * FROM merriam WHERE id = :id")
    suspend fun getRootById(id: Int): RootEntity?
}

data class MerriamRecordWithRootInfo(
    @Embedded val record: MerriamRecordEntity,
    @ColumnInfo(name = "root") val root: String,
    @ColumnInfo(name = "words") val words: List<String>,
)