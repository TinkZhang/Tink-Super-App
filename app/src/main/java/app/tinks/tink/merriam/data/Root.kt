package app.tinks.tink.merriam.data

import kotlinx.datetime.LocalDate

data class Root(
    val id: Int,
    val unit: Int,
    val text: String,
    val meaning: String? = null,
    val words: List<String> = emptyList(),
    val isCompleted: Boolean = false,
    val completeDate: LocalDate? = null,
)
