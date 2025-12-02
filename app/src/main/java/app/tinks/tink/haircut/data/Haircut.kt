package app.tinks.tink.haircut.data

import kotlinx.datetime.LocalDate

data class Haircut(
    val id: Int?,              // 这里可以用 localId 或 remoteId
    val price: Int,
    val date: LocalDate,
    val shopName: String,
)