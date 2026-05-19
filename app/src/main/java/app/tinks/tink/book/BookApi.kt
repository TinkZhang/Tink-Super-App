package app.tinks.tink.book

import app.tinks.tink.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookApi {
    @GET("book/wishlist")
    suspend fun getWishlist(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): List<BookDto>

    @POST("book/wishlist")
    suspend fun createWishlistBook(
        @Body payload: BookCreateRequest,
    ): BookDto

    @GET("book/reading")
    suspend fun getReading(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): List<BookDto>

    @GET("book/archived")
    suspend fun getArchived(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
        @Query("status") status: String? = null,
    ): List<BookDto>

    @GET("book/{bookId}")
    suspend fun getBook(@Path("bookId") bookId: Long): BookDto

    @PATCH("book/{bookId}")
    suspend fun updateBook(
        @Path("bookId") bookId: Long,
        @Body payload: BookUpdateRequest,
    ): BookDto

    @DELETE("book/{bookId}")
    suspend fun deleteBook(@Path("bookId") bookId: Long)

    @POST("book/{bookId}/reading")
    suspend fun startReading(
        @Path("bookId") bookId: Long,
        @Body payload: BookStartReadingRequest,
    ): BookDto

    @POST("book/{bookId}/archive")
    suspend fun archiveBook(
        @Path("bookId") bookId: Long,
        @Body payload: BookArchiveRequest,
    ): BookDto

    @GET("book/{bookId}/notes")
    suspend fun getNotes(
        @Path("bookId") bookId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): List<BookNoteDto>

    @POST("book/{bookId}/notes")
    suspend fun createNote(
        @Path("bookId") bookId: Long,
        @Body payload: BookNoteCreateRequest,
    ): BookNoteDto

    @PATCH("book/{bookId}/notes/{noteId}")
    suspend fun updateNote(
        @Path("bookId") bookId: Long,
        @Path("noteId") noteId: Long,
        @Body payload: BookNoteUpdateRequest,
    ): BookNoteDto

    @DELETE("book/{bookId}/notes/{noteId}")
    suspend fun deleteNote(
        @Path("bookId") bookId: Long,
        @Path("noteId") noteId: Long,
    )
}

interface GoogleBooksApi {
    @GET("books/v1/volumes")
    suspend fun search(
        @Query("q") keyword: String,
        @Query("startIndex") startIndex: Int = 0,
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") key: String = BuildConfig.GOOGLE_API_KEY,
    ): GoogleBooksResponse
}

@Serializable
data class BookDto(
    val id: Long,
    val title: String,
    val publisher: String? = null,
    val author: String? = null,
    @SerialName("cover_url")
    val coverUrl: String? = null,
    val isbn: String? = null,
    val description: String? = null,
    val rating: Double? = null,
    @SerialName("amazon_link")
    val amazonLink: String? = null,
    val pages: Int? = null,
    @SerialName("publish_year")
    val publishYear: Int? = null,
    val state: String,
    val platform: String? = null,
    @SerialName("current_page")
    val currentPage: Int? = null,
    @SerialName("progress_percentage")
    val progressPercentage: Double? = null,
    @SerialName("archive_status")
    val archiveStatus: String? = null,
    @SerialName("archived_date")
    val archivedDate: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class BookCreateRequest(
    val title: String,
    val publisher: String? = null,
    val author: String? = null,
    @SerialName("cover_url")
    val coverUrl: String? = null,
    val isbn: String? = null,
    val description: String? = null,
    val rating: Double? = null,
    @SerialName("amazon_link")
    val amazonLink: String? = null,
    val pages: Int? = null,
    @SerialName("publish_year")
    val publishYear: Int? = null,
)

@Serializable
data class BookUpdateRequest(
    val title: String? = null,
    val publisher: String? = null,
    val author: String? = null,
    @SerialName("cover_url")
    val coverUrl: String? = null,
    val isbn: String? = null,
    val description: String? = null,
    val rating: Double? = null,
    @SerialName("amazon_link")
    val amazonLink: String? = null,
    val pages: Int? = null,
    @SerialName("publish_year")
    val publishYear: Int? = null,
    val platform: String? = null,
    @SerialName("current_page")
    val currentPage: Int? = null,
    @SerialName("progress_percentage")
    val progressPercentage: Double? = null,
    @SerialName("archive_status")
    val archiveStatus: String? = null,
    @SerialName("archived_date")
    val archivedDate: String? = null,
)

@Serializable
data class BookStartReadingRequest(
    val platform: String,
    @SerialName("current_page")
    val currentPage: Int? = null,
    @SerialName("progress_percentage")
    val progressPercentage: Double? = null,
)

@Serializable
data class BookArchiveRequest(
    val status: String,
    @SerialName("archived_date")
    val archivedDate: String? = null,
)

@Serializable
data class BookNoteDto(
    val id: Long,
    @SerialName("book_id")
    val bookId: Long,
    val content: String,
    val page: Int? = null,
    @SerialName("progress_percentage")
    val progressPercentage: Double? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class BookNoteCreateRequest(
    val content: String,
    val page: Int? = null,
    @SerialName("progress_percentage")
    val progressPercentage: Double? = null,
)

@Serializable
data class BookNoteUpdateRequest(
    val content: String? = null,
    val page: Int? = null,
    @SerialName("progress_percentage")
    val progressPercentage: Double? = null,
)

@Serializable
data class GoogleBooksResponse(
    val items: List<GoogleBookItemDto> = emptyList(),
)

@Serializable
data class GoogleBookItemDto(
    val id: String,
    val volumeInfo: GoogleVolumeInfoDto = GoogleVolumeInfoDto(),
)

@Serializable
data class GoogleVolumeInfoDto(
    val title: String? = null,
    val authors: List<String> = emptyList(),
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val averageRating: Double? = null,
    val imageLinks: GoogleImageLinksDto? = null,
    val industryIdentifiers: List<GoogleIndustryIdentifierDto> = emptyList(),
    val infoLink: String? = null,
)

@Serializable
data class GoogleImageLinksDto(
    val thumbnail: String? = null,
    val smallThumbnail: String? = null,
)

@Serializable
data class GoogleIndustryIdentifierDto(
    val type: String? = null,
    val identifier: String? = null,
)

enum class BookState(val wireValue: String, val label: String) {
    Wish("wish", "Wishlist"),
    Reading("reading", "Reading"),
    Archived("archived", "Archived"),
}

enum class ArchiveStatus(val wireValue: String, val label: String) {
    Done("done", "Done"),
    Abandon("abandon", "Abandoned"),
}

data class Book(
    val id: Long,
    val title: String,
    val publisher: String?,
    val author: String?,
    val coverUrl: String?,
    val isbn: String?,
    val description: String?,
    val rating: Double?,
    val amazonLink: String?,
    val pages: Int?,
    val publishYear: Int?,
    val state: BookState,
    val platform: String?,
    val currentPage: Int?,
    val progressPercentage: Double?,
    val archiveStatus: ArchiveStatus?,
    val archivedDate: String?,
)

data class BookNote(
    val id: Long,
    val bookId: Long,
    val content: String,
    val page: Int?,
    val progressPercentage: Double?,
)

data class BookDraft(
    val title: String,
    val publisher: String? = null,
    val author: String? = null,
    val coverUrl: String? = null,
    val isbn: String? = null,
    val description: String? = null,
    val rating: Double? = null,
    val amazonLink: String? = null,
    val pages: Int? = null,
    val publishYear: Int? = null,
)

fun BookDto.toDomain(): Book = Book(
    id = id,
    title = title,
    publisher = publisher,
    author = author,
    coverUrl = coverUrl,
    isbn = isbn,
    description = description,
    rating = rating,
    amazonLink = amazonLink,
    pages = pages,
    publishYear = publishYear,
    state = when (state) {
        "wish" -> BookState.Wish
        "archived" -> BookState.Archived
        else -> BookState.Reading
    },
    platform = platform,
    currentPage = currentPage,
    progressPercentage = progressPercentage,
    archiveStatus = when (archiveStatus) {
        "abandon" -> ArchiveStatus.Abandon
        "done" -> ArchiveStatus.Done
        else -> null
    },
    archivedDate = archivedDate,
)

fun BookNoteDto.toDomain(): BookNote = BookNote(
    id = id,
    bookId = bookId,
    content = content,
    page = page,
    progressPercentage = progressPercentage,
)

fun BookDraft.toCreateRequest(): BookCreateRequest = BookCreateRequest(
    title = title,
    publisher = publisher,
    author = author,
    coverUrl = coverUrl,
    isbn = isbn,
    description = description,
    rating = rating,
    amazonLink = amazonLink,
    pages = pages,
    publishYear = publishYear,
)

fun GoogleBookItemDto.toDraft(): BookDraft {
    val info = volumeInfo
    return BookDraft(
        title = info.title.orEmpty().ifBlank { "Untitled" },
        publisher = info.publisher,
        author = info.authors.joinToString().ifBlank { null },
        coverUrl = info.imageLinks?.thumbnail?.replace("http:", "https:")
            ?: info.imageLinks?.smallThumbnail?.replace("http:", "https:"),
        isbn = info.industryIdentifiers.firstOrNull { it.type == "ISBN_13" }?.identifier
            ?: info.industryIdentifiers.firstOrNull()?.identifier,
        description = info.description,
        rating = info.averageRating,
        amazonLink = info.infoLink,
        pages = info.pageCount?.takeIf { it > 0 },
        publishYear = info.publishedDate?.take(4)?.toIntOrNull(),
    )
}
