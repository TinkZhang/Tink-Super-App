package app.tinks.tink.merriam.data

data class Root(
    val id: Int,
    val unit: Int,
    val text: String,
    val meaning: String? = null,
    val words: List<String> = emptyList(),
)
