package app.tinks.tink.story

data class Story(
    val title: String,
    val content: String?,
    val id: String,
    val createdAt: String,
    val length: Int,
    val uniqueChar: Int,
)

fun StoryDto.toDomain() = Story(
    title = title,
    content = content,
    id = id,
    createdAt = createdAt,
    length = length,
    uniqueChar = uniqueChar,
)
