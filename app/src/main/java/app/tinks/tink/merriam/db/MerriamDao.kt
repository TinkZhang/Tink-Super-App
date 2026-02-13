package app.tinks.tink.merriam.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MerriamDao {
    @Query("SELECT * FROM merriam ORDER BY id ASC")
    fun getAllRootsFlow(): Flow<List<RootEntity>>
}

//data class MerriamRecordWithRootInfo(
//    @Embedded val record: MerriamRecordEntity,
//    @ColumnInfo(name = "root") val root: String,
//    @ColumnInfo(name = "words") val words: List<String>,
//)