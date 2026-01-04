package app.tinks.tink.zi

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

// 业务逻辑数据类型
data class Zi(
    val id: Int,              // 这里可以用 localId 或 remoteId
    val zi: String,
    val lastDate: LocalDate,
    val proficiency: Int,
)

// Supabase数据类型
@Serializable
data class ZiRecord(
    val id: Int? = null,
    @SerialName("last_date")
    val lastDate: LocalDate,
    val zi: String,
    val proficiency: Int,
)

// 本地SQLite数据类型
@Entity(tableName = "zi", indices = [Index(value = ["zi"], unique = true)])
data class ZiEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    val remoteId: Int? = null,
    @ColumnInfo(name = "zi")
    val zi: String,
    val lastDate: LocalDate,
    val proficiency: Int,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)


// Entity → UI
@OptIn(ExperimentalTime::class)
fun ZiEntity.toZi(): Zi {
    return Zi(
        id = localId,
        zi = zi,
        lastDate = lastDate,
        proficiency = proficiency,
    )
}

// Record → Entity（从 Supabase 下载后写入本地）
@OptIn(ExperimentalTime::class)
fun ZiRecord.toEntity(): ZiEntity {
    return ZiEntity(
        remoteId = id,
        zi = zi,
        isSynced = true,
        lastDate = lastDate,
        proficiency = proficiency,
    )
}

// Entity → Record（上传至 Supabase）
@OptIn(ExperimentalTime::class)
fun ZiEntity.toRecord(): ZiRecord {
    return ZiRecord(
        id = remoteId,
        zi = zi,
        proficiency = proficiency,
        lastDate = lastDate,
    )
}

