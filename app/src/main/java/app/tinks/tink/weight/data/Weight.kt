package app.tinks.tink.weight.data

data class Weight(
    val id: Int,              // 这里可以用 localId 或 remoteId
    val weight: Double,
    val createdTime: Long,
)