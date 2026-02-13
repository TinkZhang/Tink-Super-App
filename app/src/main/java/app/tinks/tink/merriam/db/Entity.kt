package app.tinks.tink.merriam.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.tinks.tink.merriam.data.Root
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

// 本地SQLite数据类型
@Entity(tableName = "merriam")
data class RootEntity(
    @PrimaryKey
    val id: Int,
    val unit: Int,
    val root: String,
    val meaning: String,
    val words: List<String>,
)

@Entity(tableName = "merriam_record")
data class MerriamRecordEntity(
    @PrimaryKey
    val id: Int,
    val round: Int,
    @ColumnInfo(name = "root_id")
    val rootId: Int,
    @ColumnInfo(name = "complete_date")
    val completedDate: LocalDate? = LocalDate.fromEpochDays(1),
)

// Backend数据类型
@Serializable
data class RootRecordPostDTO(
    val id: Int,
    val completedDate: LocalDate?,
    val words: String,
    val root: String,
    val round: Int,
)

@Serializable
data class RootRecordGetDTO(
    val id: Int,
    val createDate: LocalDate?,
    val round: Int,
    val root_id: Int,
)

// Entity → UI
@OptIn(ExperimentalTime::class)
fun RootEntity.toRoot(): Root {

    return Root(
        id = id,
        unit = unit,
        text = root,
        meaning = meaning,
        words = words,
    )
}