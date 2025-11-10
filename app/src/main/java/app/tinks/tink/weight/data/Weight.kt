package app.tinks.tink.weight.data

data class Weight(
    val id: Int,              // 这里可以用 localId 或 remoteId
    val weight: Double,
    val createdAtText: String // 格式化后时间（例如 "2025-11-08 09:30"）
)