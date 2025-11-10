package app.tinks.tink.weight.data

import app.tinks.tink.weight.WeightRecord
import app.tinks.tink.weight.db.WeightEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

// Entity → UI
@OptIn(ExperimentalTime::class)
fun WeightEntity.toWeight(): Weight {
    val timeText = Instant.fromEpochMilliseconds(createdAt)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .run { "%04d-%02d-%02d %02d:%02d".format(year, monthNumber, dayOfMonth, hour, minute) }

    return Weight(
        id = localId,
        weight = weight,
        createdAtText = timeText
    )
}

// Record → Entity（从 Supabase 下载后写入本地）
@OptIn(ExperimentalTime::class)
fun WeightRecord.toEntity(): WeightEntity {
    val createdAtEpoch = createdAt
        ?.let { Instant.parse(it).toEpochMilliseconds() }
        ?: System.currentTimeMillis()

    return WeightEntity(
        remoteId = id,
        weight = weight,
        createdAt = createdAtEpoch,
        isSynced = true
    )
}

// Entity → Record（上传至 Supabase）
@OptIn(ExperimentalTime::class)
fun WeightEntity.toRecord(): WeightRecord {
    val isoTime = Instant.fromEpochMilliseconds(createdAt).toString()
    return WeightRecord(
        id = remoteId,
        createdAt = isoTime,
        weight = weight
    )
}
