package app.tinks.tink.merriam.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.tinks.tink.merriam.data.Root
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

// 本地SQLite数据类型
@Entity(tableName = "merriam")
data class RootEntity(
    val id: Int,
    val unit: Int,
    val root: String,
    val meaning: String,
    val words: List<String>,
    val completedDate: LocalDate?,
    val isSynced: Boolean,
)

// Supabase数据类型
@Serializable
data class RootRecord(
    val id: Int,
    val completedDate: LocalDate?,
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
        isCompleted = completedDate != null,
        completeDate = completedDate,
    )
}

// Entity → Record（上传至 Supabase）
@OptIn(ExperimentalTime::class)
fun RootEntity.toRecord(): RootRecord {
    return RootRecord(
        id = id,
        completedDate = completedDate,
    )
}