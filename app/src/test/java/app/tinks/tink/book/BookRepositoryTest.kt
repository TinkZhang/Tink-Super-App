package app.tinks.tink.book

import app.tinks.tink.network.ApiResult
import app.tinks.tink.time.TimeApi
import app.tinks.tink.time.TimeEntryDto
import app.tinks.tink.time.TimeLabelCreateRequest
import app.tinks.tink.time.TimeLabelDto
import app.tinks.tink.time.TimeLabelUpdateRequest
import app.tinks.tink.time.TimeStatisticDto
import app.tinks.tink.time.TimeUpsertRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class BookRepositoryTest {

    @Test
    fun bookDto_mapsReadingDuration() {
        val book = bookDto(readingDurationMinutes = 135).toDomain()

        assertEquals(135, book.readingDurationMinutes)
    }

    @Test
    fun bookNoteDto_keepsCreatedDate() {
        val note = BookNoteDto(
            id = 10,
            bookId = 1,
            content = "A useful observation",
            page = 42,
            createdAt = "2026-07-23T09:30:00Z",
        ).toDomain()

        assertEquals("2026-07-23T09:30:00Z", note.createdAt)
    }

    @Test
    fun searchBooks_usesBackendSearchAndMapsResult() = runTest {
        val api = FakeBookApi()
        val repository = BookRepository(api)

        val emissions = repository.searchBooks("Le Guin").take(2).toList()

        assertTrue(emissions.first() is ApiResult.Loading)
        val books = (emissions.last() as ApiResult.Success).data
        assertEquals(listOf("Le Guin"), api.searchQueries)
        assertEquals("The Left Hand of Darkness", books.single().title)
        assertEquals("9780441478125", books.single().isbn)
    }

    @Test
    fun searchBooks_mapsWrappedBackendSearchResults() = runTest {
        val api = FakeBookApi(
            searchResponse = buildJsonObject {
                put("content", Json.encodeToJsonElement(listOf(searchResultDto())))
            }
        )
        val repository = BookRepository(api)

        val result = repository.searchBooks("Le Guin").take(2).toList().last() as ApiResult.Success

        assertEquals("The Left Hand of Darkness", result.data.single().title)
    }

    @Test
    fun searchBooks_keepsResultWithoutSourceId() = runTest {
        val api = FakeBookApi(
            searchResponse = buildJsonArray {
                add(
                    buildJsonObject {
                        put("title", "The Dispossessed")
                        put("author", "Ursula K. Le Guin")
                    }
                )
            }
        )
        val repository = BookRepository(api)

        val result = repository.searchBooks("dispossessed").take(2).toList().last() as ApiResult.Success

        assertEquals("The Dispossessed", result.data.single().title)
    }

    @Test
    fun searchBooks_mapsRawGoogleBookItems() = runTest {
        val api = FakeBookApi(
            searchResponse = buildJsonObject {
                put(
                    "items",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("id", "google-raw")
                                put(
                                    "volumeInfo",
                                    buildJsonObject {
                                        put("title", "A Wizard of Earthsea")
                                        put("publisher", "Clarion")
                                        put("pageCount", 205)
                                        put("publishedDate", "1968-01-01")
                                        put(
                                            "authors",
                                            buildJsonArray {
                                                add("Ursula K. Le Guin")
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            }
        )
        val repository = BookRepository(api)

        val result = repository.searchBooks("earthsea").take(2).toList().last() as ApiResult.Success

        assertEquals("A Wizard of Earthsea", result.data.single().title)
        assertEquals("Ursula K. Le Guin", result.data.single().author)
        assertEquals(205, result.data.single().pages)
        assertEquals(1968, result.data.single().publishYear)
    }

    @Test
    fun addToReading_createsWishlistEntryThenStartsReading() = runTest {
        val api = FakeBookApi()
        val repository = BookRepository(api)

        val result = repository.addToReading(BookDraft(title = "A Wizard of Earthsea"), "Paper")
            .take(2)
            .toList()
            .last() as ApiResult.Success

        assertEquals("A Wizard of Earthsea", api.createdDrafts.single().title)
        assertEquals("Paper", api.startedReading.single().second.platform)
        assertEquals(null, api.startedReading.single().second.pageFormat)
        assertEquals(BookState.Reading, result.data.state)
    }

    @Test
    fun addToReading_keepsDraftPageFormat() = runTest {
        val api = FakeBookApi()
        val repository = BookRepository(api)

        repository.addToReading(
            BookDraft(title = "Digital Book", pageFormat = BookPageFormat.Percent1000),
            "Kindle",
        ).take(2).toList()

        assertEquals("percent_1000", api.startedReading.single().second.pageFormat)
    }

    @Test
    fun getBooks_passesSelectedCategoryToApi() = runTest {
        val api = FakeBookApi()
        val repository = BookRepository(api)

        repository.getBooks(BookState.Wish, "Science Fiction").take(2).toList()

        assertEquals(listOf("Science Fiction"), api.wishlistCategories)
    }

    @Test
    fun getBooks_finalShelfLoadsAbandonedAndDoneBooks() = runTest {
        val api = FakeBookApi()
        val repository = BookRepository(api)

        val result = repository.getBooks(BookState.Archived, "Memoir").take(2).toList().last() as ApiResult.Success

        assertEquals(listOf("Memoir"), api.abandonCategories)
        assertEquals(listOf("Memoir"), api.doneCategories)
        assertEquals(listOf(ArchiveStatus.Abandon, ArchiveStatus.Done), result.data.map { it.archiveStatus })
    }

    @Test
    fun getBooks_whenOffline_emitsCachedReadingShelfWithoutAnError() = runTest {
        val cachedBook = bookDto(
            title = "Cached book",
            currentPage = 42,
            pages = 210,
        ).toDomain().toEntity()
        val dao = FakeBookDao(listOf(cachedBook))
        val api = FakeBookApi(readingFailure = IOException("offline"))
        val repository = BookRepository(api, dao, null, null, null, null)

        val emissions = repository.getBooks(BookState.Reading).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions.first() is ApiResult.Loading)
        val books = (emissions.last() as ApiResult.Success).data
        assertEquals("Cached book", books.single().title)
        assertEquals(42, books.single().currentPage)
    }

    @Test
    fun queueReadingProgress_updatesCacheAndPersistsTheCompleteOfflineWrite() = runTest {
        val dao = FakeBookDao(
            listOf(
                bookDto(currentPage = 20, pages = 200, readingDurationMinutes = 30)
                    .toDomain()
                    .toEntity()
            )
        )
        val repository = BookRepository(FakeBookApi(), dao, null, null, null, null)
        val timeEntry = TimeUpsertRequest(
            type = 2,
            start = "2026-07-24T08:00:00Z",
            end = "2026-07-24T08:15:00Z",
            title = "Test Book Page 20 - Page 50",
            description = "ReadKeeper progress update",
        )

        val result = repository.queueReadingProgress(
            bookId = 1,
            currentPage = 50,
            readingDurationMinutes = 45,
            timeEntry = timeEntry,
        ).toList().last() as ApiResult.Success

        assertEquals(50, result.data.currentPage)
        assertEquals(25.0, result.data.progressPercentage)
        assertEquals(50, dao.getBook(1)?.currentPage)
        assertTrue(dao.getBook(1)?.hasPendingProgress == true)
        val queued = dao.getPendingProgress().single()
        assertEquals(45, queued.readingDurationMinutes)
        assertEquals(timeEntry.start, queued.timeStart)
        assertEquals(timeEntry.end, queued.timeEnd)
        assertEquals(timeEntry.title, queued.timeTitle)
    }

    @Test
    fun syncPendingProgress_updatesBookAndTimeThenClearsPendingState() = runTest {
        val dao = FakeBookDao(
            listOf(
                bookDto(currentPage = 20, pages = 200, readingDurationMinutes = 30)
                    .toDomain()
                    .toEntity()
            )
        )
        val api = FakeBookApi()
        val timeApi = RecordingTimeApi()
        val repository = BookRepository(api, dao, timeApi, null, null, null)
        val timeEntry = TimeUpsertRequest(
            type = 2,
            start = "2026-07-24T08:00:00Z",
            end = "2026-07-24T08:15:00Z",
            title = "Test Book Page 20 - Page 50",
        )
        repository.queueReadingProgress(
            bookId = 1,
            currentPage = 50,
            readingDurationMinutes = 45,
            timeEntry = timeEntry,
        ).toList()

        coroutineScope {
            awaitAll(
                async { repository.syncPendingProgress() },
                async { repository.syncPendingProgress() },
            )
        }

        assertEquals(50, api.updatedBooks.single().second.currentPage)
        assertEquals(45, api.updatedBooks.single().second.readingDurationMinutes)
        assertEquals(timeEntry, timeApi.createdEntries.single())
        assertTrue(dao.getPendingProgress().isEmpty())
        assertFalse(requireNotNull(dao.getBook(1)).hasPendingProgress)
    }

    private class FakeBookApi(
        private val searchResponse: JsonElement = Json.encodeToJsonElement(listOf(searchResultDto())),
        private val readingFailure: Throwable? = null,
    ) : BookApi {
        val searchQueries = mutableListOf<String>()
        val createdDrafts = mutableListOf<BookCreateRequest>()
        val startedReading = mutableListOf<Pair<Long, BookStartReadingRequest>>()
        val wishlistCategories = mutableListOf<String?>()
        val abandonCategories = mutableListOf<String?>()
        val doneCategories = mutableListOf<String?>()
        val updatedBooks = mutableListOf<Pair<Long, BookUpdateRequest>>()

        override suspend fun search(keyword: String, limit: Int): JsonElement {
            searchQueries.add(keyword)
            return searchResponse
        }

        override suspend fun createWishlistBook(payload: BookCreateRequest): BookDto {
            createdDrafts.add(payload)
            return bookDto(title = payload.title, state = "wish")
        }

        override suspend fun startReading(bookId: Long, payload: BookStartReadingRequest): BookDto {
            startedReading.add(bookId to payload)
            return bookDto(title = createdDrafts.single().title, state = "reading", platform = payload.platform)
        }

        override suspend fun getWishlist(page: Int, size: Int, category: String?): List<BookDto> {
            wishlistCategories.add(category)
            return emptyList()
        }
        override suspend fun getReading(page: Int, size: Int, category: String?): List<BookDto> {
            readingFailure?.let { throw it }
            return emptyList()
        }
        override suspend fun getAbandon(page: Int, size: Int, category: String?): List<BookDto> {
            abandonCategories.add(category)
            return listOf(bookDto(state = "abandon"))
        }
        override suspend fun getDone(page: Int, size: Int, category: String?): List<BookDto> {
            doneCategories.add(category)
            return listOf(bookDto(state = "done"))
        }
        override suspend fun getCategories(): List<String> = listOf("Science Fiction")
        override suspend fun getBook(bookId: Long): BookDto = bookDto()
        override suspend fun updateBook(bookId: Long, payload: BookUpdateRequest): BookDto {
            updatedBooks.add(bookId to payload)
            return bookDto(
                currentPage = payload.currentPage,
                readingDurationMinutes = payload.readingDurationMinutes ?: 0,
            )
        }
        override suspend fun deleteBook(bookId: Long) = Unit
        override suspend fun abandonBook(bookId: Long): BookDto = bookDto(state = "abandon")
        override suspend fun markBookDone(bookId: Long): BookDto = bookDto(state = "done")
        override suspend fun getNotes(bookId: Long, page: Int, size: Int): List<BookNoteDto> = emptyList()
        override suspend fun createNote(bookId: Long, payload: BookNoteCreateRequest): BookNoteDto =
            error("not used")
        override suspend fun updateNote(bookId: Long, noteId: Long, payload: BookNoteUpdateRequest): BookNoteDto =
            error("not used")
        override suspend fun deleteNote(bookId: Long, noteId: Long) = Unit
    }

    private class FakeBookDao(
        initialBooks: List<BookEntity> = emptyList(),
    ) : BookDao {
        private val books = initialBooks.associateByTo(linkedMapOf()) { it.id }
        private val latestReadingBook = MutableStateFlow(findLatestReadingBook())
        private val pending = mutableListOf<BookPendingProgressEntity>()
        private var nextPendingId = 1L

        override suspend fun getBooks(states: List<String>): List<BookEntity> =
            books.values.filter { it.state in states }

        override suspend fun getBook(bookId: Long): BookEntity? = books[bookId]

        override fun observeLatestReadingBook(): Flow<BookEntity?> = latestReadingBook

        override suspend fun getLatestReadingBook(): BookEntity? = findLatestReadingBook()

        override suspend fun upsertBook(book: BookEntity) {
            books[book.id] = book
            emitLatestReadingBook()
        }

        override suspend fun upsertBooks(books: List<BookEntity>) {
            books.forEach { book -> this.books[book.id] = book }
            emitLatestReadingBook()
        }

        override suspend fun deleteBook(bookId: Long) {
            books.remove(bookId)
            emitLatestReadingBook()
        }

        override suspend fun deleteSyncedBooksForState(state: String) {
            books.entries.removeAll { (_, book) ->
                book.state == state && !book.hasPendingProgress
            }
            emitLatestReadingBook()
        }

        override suspend fun applyLocalProgress(
            bookId: Long,
            currentPage: Int?,
            readingDurationMinutes: Int,
            updatedAt: String,
        ) {
            books[bookId] = requireNotNull(books[bookId]).copy(
                currentPage = currentPage,
                readingDurationMinutes = readingDurationMinutes,
                updatedAt = updatedAt,
                hasPendingProgress = true,
            )
            emitLatestReadingBook()
        }

        override suspend fun clearPendingProgress(bookId: Long) {
            books[bookId] = requireNotNull(books[bookId]).copy(hasPendingProgress = false)
            emitLatestReadingBook()
        }

        override suspend fun insertPendingProgress(action: BookPendingProgressEntity): Long {
            val id = nextPendingId++
            pending.add(action.copy(id = id))
            return id
        }

        override suspend fun getPendingProgress(): List<BookPendingProgressEntity> =
            pending.toList()

        override suspend fun markBookSynced(actionId: Long) {
            updatePending(actionId) { it.copy(bookSynced = true) }
        }

        override suspend fun markTimeSynced(actionId: Long) {
            updatePending(actionId) { it.copy(timeSynced = true) }
        }

        override suspend fun deletePendingProgress(actionId: Long) {
            pending.removeAll { it.id == actionId }
        }

        override suspend fun pendingProgressCount(bookId: Long): Int =
            pending.count { it.bookId == bookId }

        override suspend fun deletePendingProgressForBook(bookId: Long) {
            pending.removeAll { it.bookId == bookId }
        }

        private fun updatePending(
            actionId: Long,
            transform: (BookPendingProgressEntity) -> BookPendingProgressEntity,
        ) {
            val index = pending.indexOfFirst { it.id == actionId }
            pending[index] = transform(pending[index])
        }

        private fun findLatestReadingBook(): BookEntity? =
            books.values
                .filter { it.state == BookState.Reading.wireValue }
                .maxByOrNull { it.updatedAt ?: it.createdAt.orEmpty() }

        private fun emitLatestReadingBook() {
            latestReadingBook.value = findLatestReadingBook()
        }
    }

    private class RecordingTimeApi : TimeApi {
        val createdEntries = mutableListOf<TimeUpsertRequest>()

        override suspend fun getStatistics(
            startDate: String,
            endDate: String,
        ): List<TimeStatisticDto> = emptyList()

        override suspend fun getTimeEntries(
            startDate: String,
            endDate: String,
        ): List<TimeEntryDto> = emptyList()

        override suspend fun createTimeEntry(payload: TimeUpsertRequest): TimeEntryDto {
            createdEntries.add(payload)
            return payload.toDto(id = createdEntries.size.toLong())
        }

        override suspend fun updateTimeEntry(
            timeId: Long,
            payload: TimeUpsertRequest,
        ): TimeEntryDto = payload.toDto(id = timeId)

        override suspend fun deleteTimeEntry(timeId: Long) = Unit

        override suspend fun getTimeLabels(type: Int?): List<TimeLabelDto> = emptyList()

        override suspend fun createTimeLabel(payload: TimeLabelCreateRequest): TimeLabelDto =
            error("not used")

        override suspend fun updateTimeLabel(
            labelId: Long,
            payload: TimeLabelUpdateRequest,
        ): TimeLabelDto = error("not used")

        override suspend fun deleteTimeLabel(labelId: Long) = Unit

        private fun TimeUpsertRequest.toDto(id: Long): TimeEntryDto =
            TimeEntryDto(
                id = id,
                type = type,
                start = start,
                end = end,
                title = title,
                description = description,
                allDay = allDay,
                includeInStatistics = includeInStatistics,
                source = source,
            )
    }
}

private fun searchResultDto() = BookSearchResultDto(
    sourceId = "google-1",
    title = "The Left Hand of Darkness",
    author = "Ursula K. Le Guin",
    isbn = "9780441478125",
)

private fun bookDto(
    title: String = "Test Book",
    state: String = "reading",
    platform: String? = null,
    readingDurationMinutes: Int = 0,
    currentPage: Int? = null,
    pages: Int? = null,
) = BookDto(
    id = 1,
    title = title,
    state = state,
    platform = platform,
    readingDurationMinutes = readingDurationMinutes,
    currentPage = currentPage,
    pages = pages,
    createdAt = "2026-05-25T08:00:00Z",
)
