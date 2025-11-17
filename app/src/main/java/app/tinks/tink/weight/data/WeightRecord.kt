package app.tinks.tink.weight.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeightRecord(
    val id: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val weight: Double
)