package app.tinks.tink.book

import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import app.tinks.tink.time.TimeApi
import app.tinks.tink.time.TimeUpsertRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val bookSearchJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Singleton
open class BookRepository @Inject constructor(
    private val bookApi: BookApi,
    private val bookDao: BookDao?,
    private val timeApi: TimeApi?,
    private val syncScheduler: ReadKeeperSyncScheduler?,
    private val coverCache: BookCoverCache?,
    private val widgetUpdater: ReadKeeperWidgetUpdater?,
) {
    private val syncMutex = Mutex()

    constructor(bookApi: BookApi) : this(bookApi, null, null, null, null, null)

    open fun getBooks(state: BookState, category: String? = null): Flow<ApiResult<List<Book>>> = flow {
        emit(ApiResult.Loading)
        val cached = bookDao
            ?.getBooks(state.cacheStates())
            .orEmpty()
            .map { it.toDomain() }
            .filterByCategory(category)
        if (cached.isNotEmpty()) emit(ApiResult.Success(cached))

        when (val remote = safeApiCall { fetchRemoteBooks(state, category) }) {
            is ApiResult.Success -> {
                cacheBooks(state, remote.data)
                val current = if (state == BookState.Reading && bookDao != null) {
                    bookDao.getBooks(state.cacheStates()).map { it.toDomain() }.filterByCategory(category)
                } else remote.data
                emit(ApiResult.Success(current))
            }
            is ApiResult.Error -> if (cached.isEmpty()) emit(remote)
            ApiResult.Loading -> Unit
        }
    }.flowOn(Dispatchers.IO)

    open fun getCategories(): Flow<ApiResult<List<String>>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall { bookApi.getCategories() })
    }.flowOn(Dispatchers.IO)

    open fun getBook(bookId: Long): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        val cached = bookDao?.getBook(bookId)?.toDomain()
        if (cached != null) emit(ApiResult.Success(cached))
        when (val remote = safeApiCall { bookApi.getBook(bookId).toDomain() }) {
            is ApiResult.Success -> {
                emit(ApiResult.Success(cacheRemoteBook(remote.data)))
            }
            is ApiResult.Error -> if (cached == null) emit(remote)
            ApiResult.Loading -> Unit
        }
    }.flowOn(Dispatchers.IO)

    open fun addToWishlist(draft: BookDraft): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        val result = safeApiCall { bookApi.createWishlistBook(draft.toCreateRequest()).toDomain() }
        if (result is ApiResult.Success) cacheRemoteBook(result.data)
        emit(result)
    }.flowOn(Dispatchers.IO)

    open fun addToReading(
        draft: BookDraft,
        platform: String = "General",
    ): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                val created = bookApi.createWishlistBook(draft.toCreateRequest())
                val book = bookApi.startReading(
                    bookId = created.id,
                    payload = BookStartReadingRequest(
                        platform = platform,
                        currentPage = 0,
                        pageFormat = draft.pageFormat.takeUnless { it == BookPageFormat.Page }?.wireValue,
                    )
                ).toDomain()
                cacheRemoteBook(book)
                book
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun moveToReading(
        bookId: Long,
        platform: String,
        pageFormat: BookPageFormat = BookPageFormat.Page,
        currentPage: Int? = null,
    ): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                val book = bookApi.startReading(
                    bookId = bookId,
                    payload = BookStartReadingRequest(
                        platform = platform,
                        currentPage = currentPage,
                        pageFormat = pageFormat.takeUnless { it == BookPageFormat.Page }?.wireValue,
                    )
                ).toDomain()
                cacheRemoteBook(book)
                book
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun updateBook(bookId: Long, payload: BookUpdateRequest): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        val result = safeApiCall { bookApi.updateBook(bookId, payload).toDomain() }
        if (result is ApiResult.Success) cacheRemoteBook(result.data)
        emit(result)
    }.flowOn(Dispatchers.IO)

    open fun queueReadingProgress(
        bookId: Long,
        currentPage: Int?,
        readingDurationMinutes: Int,
        timeEntry: TimeUpsertRequest?,
    ): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        val dao = bookDao
        if (dao == null) {
            val result = safeApiCall {
                timeEntry?.let { requireNotNull(timeApi).createTimeEntry(it) }
                bookApi.updateBook(
                    bookId,
                    BookUpdateRequest(
                        currentPage = currentPage,
                        readingDurationMinutes = readingDurationMinutes,
                    ),
                ).toDomain()
            }
            emit(result)
            return@flow
        }

        val cached = dao.getBook(bookId)?.toDomain()
            ?: runCatching { bookApi.getBook(bookId).toDomain() }
                .getOrNull()
                ?.also { cacheRemoteBook(it) }
        if (cached == null) {
            emit(ApiResult.Error("No cached book available"))
            return@flow
        }

        val updatedAt = Instant.now().toString()
        dao.enqueueProgress(
            bookId = bookId,
            currentPage = currentPage,
            readingDurationMinutes = readingDurationMinutes,
            updatedAt = updatedAt,
            action = BookPendingProgressEntity(
                bookId = bookId,
                currentPage = currentPage,
                readingDurationMinutes = readingDurationMinutes,
                timeType = timeEntry?.type,
                timeStart = timeEntry?.start,
                timeEnd = timeEntry?.end,
                timeTitle = timeEntry?.title,
                timeDescription = timeEntry?.description,
                timeSynced = timeEntry == null,
            ),
        )
        syncScheduler?.enqueue()
        updateWidget()
        emit(
            ApiResult.Success(
                cached.copy(
                    currentPage = currentPage,
                    progressPercentage = currentPage.toProgressPercentage(cached.pageFormat, cached.pages),
                    readingDurationMinutes = readingDurationMinutes,
                    updatedAt = updatedAt,
                )
            )
        )
    }.flowOn(Dispatchers.IO)

    open fun archiveBook(
        bookId: Long,
        status: ArchiveStatus = ArchiveStatus.Done,
    ): Flow<ApiResult<Book>> = flow {
        emit(ApiResult.Loading)
        emit(
            safeApiCall {
                val book = when (status) {
                    ArchiveStatus.Abandon -> bookApi.abandonBook(bookId)
                    ArchiveStatus.Done -> bookApi.markBookDone(bookId)
                }.toDomain()
                cacheRemoteBook(book)
                book
            }
        )
    }.flowOn(Dispatchers.IO)

    open fun deleteBook(bookId: Long): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        val result = safeApiCall { bookApi.deleteBook(bookId) }
        if (result is ApiResult.Success) {
            bookDao?.deleteBookAndPendingProgress(bookId)
            updateWidget()
        }
        emit(result)
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

    open suspend fun syncPendingProgress() = syncMutex.withLock {
        drainPendingProgress()
    }

    open suspend fun synchronize() = syncMutex.withLock {
        drainPendingProgress()
        refreshAllBooks()
        // Pick up progress added while the refresh was running.
        drainPendingProgress()
    }

    private suspend fun drainPendingProgress() {
        val dao = bookDao ?: return
        val api = timeApi ?: return
        dao.getPendingProgress().forEach { queued ->
            var action = queued
            if (!action.bookSynced) {
                bookApi.updateBook(
                    action.bookId,
                    BookUpdateRequest(
                        currentPage = action.currentPage,
                        readingDurationMinutes = action.readingDurationMinutes,
                    ),
                )
                dao.markBookSynced(action.id)
                action = action.copy(bookSynced = true)
            }
            if (!action.timeSynced) {
                api.createTimeEntry(
                    TimeUpsertRequest(
                        type = requireNotNull(action.timeType),
                        start = requireNotNull(action.timeStart),
                        end = requireNotNull(action.timeEnd),
                        title = requireNotNull(action.timeTitle),
                        description = action.timeDescription,
                    )
                )
                dao.markTimeSynced(action.id)
            }
            dao.deletePendingProgress(action.id)
            if (dao.pendingProgressCount(action.bookId) == 0) {
                dao.clearPendingProgress(action.bookId)
            }
        }
    }

    open suspend fun refreshAllBooks() {
        cacheBooks(BookState.Wish, fetchRemoteBooks(BookState.Wish, null))
        cacheBooks(BookState.Reading, fetchRemoteBooks(BookState.Reading, null))
        cacheBooks(BookState.Archived, fetchRemoteBooks(BookState.Archived, null))
    }

    open suspend fun refreshReadingBooks() {
        cacheBooks(BookState.Reading, fetchRemoteBooks(BookState.Reading, null))
    }

    private suspend fun fetchRemoteBooks(state: BookState, category: String?): List<Book> =
        when (state) {
            BookState.Wish -> bookApi.getWishlist(category = category)
            BookState.Reading -> bookApi.getReading(category = category)
            BookState.Archived -> bookApi.getAbandon(category = category) + bookApi.getDone(category = category)
        }.map { it.toDomain() }

    private suspend fun cacheBooks(state: BookState, books: List<Book>) {
        bookDao?.replaceRemoteBooks(
            states = state.cacheStates(),
            books = books.map { it.toEntity() },
        )
        if (state == BookState.Reading) {
            cacheCover(bookDao?.getLatestReadingBook()?.coverUrl)
            updateWidget()
        }
    }

    private suspend fun cacheRemoteBook(book: Book): Book {
        val dao = bookDao ?: return book
        val local = dao.getBook(book.id)
        val remote = if (local?.hasPendingProgress == true) {
            book.toEntity(hasPendingProgress = true).copy(
                currentPage = local.currentPage,
                readingDurationMinutes = local.readingDurationMinutes,
                updatedAt = local.updatedAt,
            )
        } else {
            book.toEntity()
        }
        dao.upsertBook(remote)
        if (local?.state == BookState.Reading.wireValue || remote.state == BookState.Reading.wireValue) {
            cacheCover(remote.coverUrl)
            updateWidget()
        }
        return if (local?.hasPendingProgress == true) remote.toDomain() else book
    }

    private fun cacheCover(url: String?) {
        coverCache?.cacheInBackground(url) {
            updateWidget()
        }
    }

    private suspend fun updateWidget() {
        runCatching { widgetUpdater?.update() }
    }
}

private fun List<Book>.filterByCategory(category: String?): List<Book> =
    category?.let { selected -> filter { it.category == selected } } ?: this

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
