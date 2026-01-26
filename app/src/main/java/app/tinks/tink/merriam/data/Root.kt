package app.tinks.tink.merriam.data

import kotlinx.datetime.LocalDate

data class Root(
    val text: String,
    val meaning: String? = null,
    val words: List<String> = emptyList(),
    val isCompleted: Boolean = false,
    val completeDate: LocalDate? = null,
)
