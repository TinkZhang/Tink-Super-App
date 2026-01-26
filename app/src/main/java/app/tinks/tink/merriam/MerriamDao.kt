package app.tinks.tink.merriam

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.tinks.tink.zi.ZiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZiDao {

    @Query("SELECT * FROM Zi WHERE isDeleted = 0 ORDER BY proficiency DESC")
    fun getAllZisFlow(): Flow<List<ZiEntity>>

    @Query("SELECT * FROM Zi WHERE isDeleted = 0 AND proficiency = 5 ORDER BY lastDate DESC")
    fun getAllLearntZisFlow(): Flow<List<ZiEntity>>

    @Query("SELECT * FROM Zi WHERE isDeleted = 0 ORDER BY proficiency DESC")
    suspend fun getAllZis(): List<ZiEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZi(zi: ZiEntity)

    @Update
    suspend fun updateZi(zi: ZiEntity)

    @Query("UPDATE Zi SET isDeleted = 1, isSynced = 0 WHERE localId = :id")
    suspend fun markDeleted(id: Int)

    @Query("DELETE FROM Zi WHERE localId = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM Zi WHERE isSynced = 0")
    suspend fun getUnsyncedZis(): List<ZiEntity>

    @Query("UPDATE Zi SET isSynced = 1 WHERE localId = :id")
    suspend fun markSynced(id: Int)

    @Query("DELETE FROM Zi")
    suspend fun clearAll()
}