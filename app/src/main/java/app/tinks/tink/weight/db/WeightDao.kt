package app.tinks.tink.weight.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface WeightDao {

    @Query("SELECT * FROM weight WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllWeightsFlow(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weight WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllWeights(): List<WeightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightEntity)

    @Update
    suspend fun updateWeight(weight: WeightEntity)

    @Query("UPDATE weight SET isDeleted = 1, isSynced = 0 WHERE localId = :id")
    suspend fun markDeleted(id: Int)

    @Query("DELETE FROM weight WHERE localId = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM weight WHERE isSynced = 0")
    suspend fun getUnsyncedWeights(): List<WeightEntity>

    @Query("UPDATE weight SET isSynced = 1 WHERE localId = :id")
    suspend fun markSynced(id: Int)

    @Query("DELETE FROM weight")
    suspend fun clearAll()
}
