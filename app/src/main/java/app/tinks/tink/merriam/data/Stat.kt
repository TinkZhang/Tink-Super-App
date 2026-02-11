package app.tinks.tink.merriam.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatDto(
    val latest: Int,
    @SerialName("week_stats")
    val weekStats: List<Int?>
) {
    fun toDomain(): Stat = Stat(latest, weekStats)
}

data class Stat(val latest: Int, val weeeStats: List<Int?>)
