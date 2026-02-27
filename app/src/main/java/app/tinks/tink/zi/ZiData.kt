package app.tinks.tink.zi

import kotlinx.datetime.LocalDate

// Domain data type used by UI.
data class Zi(
    val zi: String,
    val lastDate: LocalDate,
    val proficiency: Int,
)

fun ZiDto.toDomain(): Zi {
    val parsedDate = runCatching { LocalDate.parse(lastDate) }
        .getOrElse { LocalDate(1970, 1, 1) }

    return Zi(
        zi = zi,
        lastDate = parsedDate,
        proficiency = proficiency,
    )
}
