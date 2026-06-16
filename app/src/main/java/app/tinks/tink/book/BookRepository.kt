package app.tinks.tink.book

import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

private val bookSearchJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Singleton
open class BookRepository @Inject constructor(
    private val bookApi: BookApi,
) {
    open fun getBooks(state: BookState, category: String? = null): Flow<ApiResult<List<Book>>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                when (state) {
                    BookState.Wish -> bookApi.getWishlist(category = category)
                    BookState.Reading -> bookApi.getReading(category = category)
                    BookState.Archived -> bookApi.getArchived(category = category)
                }.map { it.toDomain() }
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun getCategories(): Flow<ApiResult<List<String>>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.getCategories() })
    }.flowOn(Dispatchers.IO)

    open fun getBook(bookId: Long): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.getBook(bookId).toDomain() })
    }.flowOn(Dispatchers.IO)

    open fun addToWishlist(draft: BookDraft): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.createWishlistBook(draft.toCreateRequest()).toDomain() })
    }.flowOn(Dispatchers.IO)

    open fun addToReading(
        draft: BookDraft,
        platform: String = "General",
    ): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                val created = bookApi.createWishlistBook(draft.toCreateRequest())
                bookApi.startReading(
                    bookId = created.id,
                    payload = BookStartReadingRequest(
                        platform = platform,
                        currentPage = 0,
                        progressPercentage = 0.0,
                        pageFormat = draft.pageFormat.takeUnless { it == BookPageFormat.Page }?.wireValue,
                    )
                ).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun moveToReading(
        bookId: Long,
        platform: String,
        pageFormat: BookPageFormat = BookPageFormat.Page,
        currentPage: Int? = null,
        progressPercentage: Double? = null,
    ): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                bookApi.startReading(
                    bookId = bookId,
                    payload = BookStartReadingRequest(
                        platform = platform,
                        currentPage = currentPage,
                        progressPercentage = progressPercentage,
                        pageFormat = pageFormat.takeUnless { it == BookPageFormat.Page }?.wireValue,
                    )
                ).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun updateBook(bookId: Long, payload: BookUpdateRequest): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.updateBook(bookId, payload).toDomain() })
    }.flowOn(Dispatchers.IO)

    open fun archiveBook(
        bookId: Long,
        status: ArchiveStatus = ArchiveStatus.Done,
    ): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                bookApi.archiveBook(
                    bookId = bookId,
                    payload = BookArchiveRequest(status = status.wireValue),
                ).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun deleteBook(bookId: Long): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.deleteBook(bookId) })
    }.flowOn(Dispatchers.IO)

    open fun getNotes(bookId: Long): Flow<ApiResult<List<BookNote>>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.getNotes(bookId).map { it.toDomain() } })
    }.flowOn(Dispatchers.IO)

    open fun saveNote(
        bookId: Long,
        noteId: Long?,
        content: String,
        page: Int?,
        progressPercentage: Double?,
    ): Flow<ApiResult<BookNote>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                if (noteId == null) {
                    bookApi.createNote(
                        bookId = bookId,
                        payload = BookNoteCreateRequest(
                            content = content,
                            page = page,
                            progressPercentage = progressPercentage,
                        ),
                    )
                } else {
                    bookApi.updateNote(
                        bookId = bookId,
                        noteId = noteId,
                        payload = BookNoteUpdateRequest(
                            content = content,
                            page = page,
                            progressPercentage = progressPercentage,
                        ),
                    )
                }.toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun deleteNote(bookId: Long, noteId: Long): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.deleteNote(bookId, noteId) })
    }.flowOn(Dispatchers.IO)

    open fun searchBooks(keyword: String): Flow<ApiResult<List<BookDraft>>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                bookApi.search(keyword = keyword.trim())
                    .toSearchResults()
                    .map { it.toDraft() }
                    .filter { it.title.isNotBlank() }
            }
        )
    }.flowOn(Dispatchers.IO)
}

private fun JsonElement.toSearchResults(): List<BookSearchResultDto> {
    val results = when (this) {
        is JsonArray -> this
        is JsonObject -> listOf("items", "content", "results", "data")
            .firstNotNullOfOrNull { this[it] as? JsonArray }
        else -> null
    } ?: return emptyList()

    return results.mapNotNull { element ->
        val decoded = runCatching {
            bookSearchJson.decodeFromJsonElement<BookSearchResultDto>(element)
        }.getOrNull()
        decoded?.takeIf { it.title.isNotBlank() } ?: (element as? JsonObject)?.toFlexibleSearchResult()
    }
}

private fun JsonObject.toFlexibleSearchResult(): BookSearchResultDto? {
    val volumeInfo = this["volumeInfo"] as? JsonObject
    val source = volumeInfo ?: this
    val title = source.stringValue("title")?.takeIf { it.isNotBlank() } ?: return null
    val identifiers = volumeInfo
        ?.get("industryIdentifiers")
        ?.jsonArrayOrNull()
        .orEmpty()
        .mapNotNull { it as? JsonObject }
    val isbn = this.stringValue("isbn")
        ?: identifiers.firstOrNull { it.stringValue("type") == "ISBN_13" }?.stringValue("identifier")
        ?: identifiers.firstNotNullOfOrNull { it.stringValue("identifier") }
    val authors = volumeInfo
        ?.get("authors")
        ?.jsonArrayOrNull()
        .orEmpty()
        .mapNotNull { it.jsonPrimitive.contentOrNull }
    val publishedDate = source.stringValue("publishedDate")
    val coverUrl = this.stringValue("cover_url")
        ?: this.stringValue("coverUrl")
        ?: (source["imageLinks"] as? JsonObject)?.stringValue("thumbnail")

    return BookSearchResultDto(
        sourceId = this.stringValue("source_id") ?: this.stringValue("sourceId") ?: this.stringValue("id").orEmpty(),
        title = title,
        publisher = source.stringValue("publisher"),
        author = this.stringValue("author") ?: authors.takeIf { it.isNotEmpty() }?.joinToString(", "),
        coverUrl = coverUrl?.replace("http://", "https://"),
        isbn = isbn,
        description = source.stringValue("description"),
        rating = source.doubleValue("rating") ?: source.doubleValue("averageRating"),
        amazonLink = this.stringValue("amazon_link") ?: this.stringValue("amazonLink") ?: source.stringValue("infoLink"),
        pages = source.intValue("pages") ?: source.intValue("pageCount"),
        publishYear = source.intValue("publish_year")
            ?: source.intValue("publishYear")
            ?: publishedDate?.take(4)?.toIntOrNull(),
    )
}

private fun JsonObject.stringValue(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull

private fun JsonObject.intValue(key: String): Int? =
    this[key]?.jsonPrimitive?.intOrNull

private fun JsonObject.doubleValue(key: String): Double? =
    this[key]?.jsonPrimitive?.doubleOrNull

private fun JsonElement.jsonArrayOrNull(): JsonArray? =
    runCatching { jsonArray }.getOrNull()
