package app.tinks.tink.merriam.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MerriamDao {
    @Query("SELECT * FROM merriam ORDER BY id DESC")
    fun getAllRootsFlow(): Flow<List<RootEntity>>
}