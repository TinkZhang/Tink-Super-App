package app.tinks.tink.book

import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookApi: BookApi,
    private val googleBooksApi: GoogleBooksApi,
) {
    fun getBooks(state: BookState): Flow<ApiResult<List<Book>>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                when (state) {
                    BookState.Wish -> bookApi.getWishlist()
                    BookState.Reading -> bookApi.getReading()
                    BookState.Archived -> bookApi.getArchived()
                }.map { it.toDomain() }
            }
        )
    }.flowOn(Dispatchers.IO)

    fun getBook(bookId: Long): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.getBook(bookId).toDomain() })
    }.flowOn(Dispatchers.IO)

    fun addToWishlist(draft: BookDraft): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.createWishlistBook(draft.toCreateRequest()).toDomain() })
    }.flowOn(Dispatchers.IO)

    fun addToReading(
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
                    )
                ).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    fun moveToReading(
        bookId: Long,
        platform: String,
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
                    )
                ).toDomain()
            }
        )
    }.flowOn(Dispatchers.IO)

    fun updateBook(bookId: Long, payload: BookUpdateRequest): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.updateBook(bookId, payload).toDomain() })
    }.flowOn(Dispatchers.IO)

    fun archiveBook(
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

    fun deleteBook(bookId: Long): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.deleteBook(bookId) })
    }.flowOn(Dispatchers.IO)

    fun getNotes(bookId: Long): Flow<ApiResult<List<BookNote>>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.getNotes(bookId).map { it.toDomain() } })
    }.flowOn(Dispatchers.IO)

    fun saveNote(
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

    fun deleteNote(bookId: Long, noteId: Long): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.deleteNote(bookId, noteId) })
    }.flowOn(Dispatchers.IO)

    fun searchGoogleBooks(keyword: String): Flow<ApiResult<List<BookDraft>>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                googleBooksApi.search(keyword = keyword.trim())
                    .items
                    .map { it.toDraft() }
                    .filter { it.title.isNotBlank() }
            }
        )
    }.flowOn(Dispatchers.IO)
}
