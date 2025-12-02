package app.tinks.tink.haircut.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface HaircutDao {

    @Query("SELECT * FROM haircut WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllHaircutsFlow(): Flow<List<HaircutEntity>>

    @Query("SELECT * FROM haircut WHERE isDeleted = 0 ORDER BY date DESC")
    suspend fun getAllHaircuts(): List<HaircutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHaircut(haircut: HaircutEntity)

    @Update
    suspend fun updateHaircut(haircut: HaircutEntity)

    @Query("UPDATE haircut SET isDeleted = 1, isSynced = 0 WHERE localId = :id")
    suspend fun markDeleted(id: Int)

    @Query("DELETE FROM haircut WHERE localId = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM haircut WHERE isSynced = 0")
    suspend fun getUnsyncedHaircuts(): List<HaircutEntity>

    @Query("UPDATE haircut SET isSynced = 1 WHERE localId = :id")
    suspend fun markSynced(id: Int?)

    @Query("DELETE FROM haircut")
    suspend fun clearAll()
}
