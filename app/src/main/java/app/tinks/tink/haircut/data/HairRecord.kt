package app.tinks.tink.haircut.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HairRecord(
    val id: Int?,
    @SerialName("created_at")
    val date: LocalDate,
    @SerialName("ship_name")
    val shopName: String,
    val price: Int,
)