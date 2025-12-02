package app.tinks.tink.haircut.data

import app.tinks.tink.haircut.db.HaircutEntity
import kotlin.time.ExperimentalTime

// Entity → UI
@OptIn(ExperimentalTime::class)
fun HaircutEntity.toHaircut(): Haircut {
    return Haircut(
        id = localId,
        price = price,
        date = date,
        shopName = shopName,
    )
}

// Record → Entity（从 Supabase 下载后写入本地）
@OptIn(ExperimentalTime::class)
fun HairRecord.toEntity(): HaircutEntity {
    return HaircutEntity(
        remoteId = id,
        price = price,
        shopName = shopName,
        date = date,
        isSynced = true
    )
}

// Entity → Record（上传至 Supabase）
@OptIn(ExperimentalTime::class)
fun HaircutEntity.toRecord(): HairRecord {
    return HairRecord(
        id = remoteId,
        date = date,
        price = price,
        shopName = shopName,
    )
}
