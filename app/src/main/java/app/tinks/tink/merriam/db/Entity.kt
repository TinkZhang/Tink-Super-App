package app.tinks.tink.merriam.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.tinks.tink.merriam.data.Root
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

// 本地SQLite数据类型
@Entity(tableName = "root")
data class RootEntity @OptIn(ExperimentalUuidApi::class) constructor(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val unitId: Int,
    val text: String,
    val meaning: String,
    val words: List<String>,
    val completedDate: LocalDate?,
    val isSynced: Boolean,
)

// Supabase数据类型
@Serializable
data class RootRecord(
    val id: Int,
    val unitId: Int,
    val text: String,
    val meaning: String,
    val words: List<String>,
    val completedDate: LocalDate?,
)

// Entity → UI
@OptIn(ExperimentalTime::class)
fun RootEntity.toRoot(): Root {
    return Root(
        text = text,
        meaning = meaning,
        words = words,
        isCompleted = completedDate != null,
        completeDate = completedDate,
    )
}

// Record → Entity（从 Supabase 下载后写入本地）
@OptIn(ExperimentalTime::class)
fun RootRecord.toEntity(): RootEntity {
    return RootEntity(
        id = id,
        unitId = unitId,
        text = text,
        meaning = meaning,
        words = words,
        completedDate = completedDate,
        isSynced = true,
    )
}

// Entity → Record（上传至 Supabase）
@OptIn(ExperimentalTime::class)
fun RootEntity.toRecord(): RootRecord {
    return RootRecord(
        id = id,
        unitId = unitId,
        text = text,
        meaning = meaning,
        words = words,
        completedDate = completedDate,
    )
}