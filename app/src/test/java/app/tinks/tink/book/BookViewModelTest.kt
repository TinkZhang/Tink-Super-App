package app.tinks.tink.book

import app.tinks.tink.network.ApiResult
import app.tinks.tink.testing.MainDispatcherRule
import app.tinks.tink.time.TimeApi
import app.tinks.tink.time.TimeDashboard
import app.tinks.tink.time.TimeEntry
import app.tinks.tink.time.TimeEntryDto
import app.tinks.tink.time.TimeLabelCreateRequest
import app.tinks.tink.time.TimeLabelDto
import app.tinks.tink.time.TimeLabelUpdateRequest
import app.tinks.tink.time.TimeRepository
import app.tinks.tink.time.TimeStatisticDto
import app.tinks.tink.time.TimeUpsertRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

class BookViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsThreeShelves() = runTest {
        val viewModel = BookViewModel(FakeRepository(), FakeTimeRepository())

        assertEquals("Currently reading", viewModel.uiState.value.readingBooks.single().title)
        assertEquals("Next book", viewModel.uiState.value.wishlistBooks.single().title)
        assertEquals("Finished book", viewModel.uiState.value.archivedBooks.single().title)
    }

    @Test
    fun init_loadsCurrentYearReadingRecordDates() = runTest {
        val readingDate = LocalDate.now().withDayOfYear(12)
        val otherDate = readingDate.plusDays(1)
        val viewModel = BookViewModel(
            FakeRepository(),
            FakeTimeRepository(
                entries = listOf(
                    timeEntry(type = 2, start = readingDate.atStartOfDay().atOffset(OffsetDateTime.now().offset)),
                    timeEntry(type = 1, start = otherDate.atStartOfDay().atOffset(OffsetDateTime.now().offset)),
                )
            )
        )
        advanceUntilIdle()

        assertEquals(setOf(readingDate), viewModel.uiState.value.readingRecordDates)
    }

    @Test
    fun listAndDetailNavigation_canReturnToPreviousScreens() = runTest {
        val viewModel = BookViewModel(FakeRepository(), FakeTimeRepository())

        viewModel.onEvent(BookEvent.OpenList(BookState.Reading))
        viewModel.onEvent(BookEvent.OpenDetail(1))
        viewModel.onEvent(BookEvent.NavigateBack)
        assertEquals(BooksScreenState.List(BookState.Reading), viewModel.uiState.value.screen)

        viewModel.onEvent(BookEvent.NavigateBack)
        assertEquals(BooksScreenState.Home, viewModel.uiState.value.screen)
    }

    @Test
    fun selectingCategory_filtersTheOpenShelf() = runTest {
        val repository = FakeRepository()
        val viewModel = BookViewModel(repository, FakeTimeRepository())

        viewModel.onEvent(BookEvent.OpenList(BookState.Reading))
        viewModel.onEvent(BookEvent.SelectCategory("Science Fiction"))

        assertEquals("Science Fiction", viewModel.uiState.value.selectedCategory)
        assertEquals(BookState.Reading to "Science Fiction", repository.requests.last())
    }

    @Test
    fun submitSearch_populatesSearchResults() = runTest {
        val viewModel = BookViewModel(FakeRepository(), FakeTimeRepository())

        viewModel.onEvent(BookEvent.OpenSearch)
        viewModel.onEvent(BookEvent.SearchKeywordChanged("earthsea"))
        viewModel.onEvent(BookEvent.SubmitSearch)
        advanceUntilIdle()

        assertEquals(BooksScreenState.SearchResults("earthsea"), viewModel.uiState.value.screen)
        assertEquals("A Wizard of Earthsea", viewModel.uiState.value.searchResults.single().title)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun navigatingBackFromSearchResults_returnsHomeAndClearsSearch() = runTest {
        val viewModel = BookViewModel(FakeRepository(), FakeTimeRepository())

        viewModel.onEvent(BookEvent.OpenSearch)
        viewModel.onEvent(BookEvent.SearchKeywordChanged("earthsea"))
        viewModel.onEvent(BookEvent.SubmitSearch)
        advanceUntilIdle()

        viewModel.onEvent(BookEvent.NavigateBack)
        advanceUntilIdle()

        assertEquals(BooksScreenState.Home, viewModel.uiState.value.screen)
        assertEquals(listOf(BooksScreenState.Home), viewModel.uiState.value.navigationStack)
        assertEquals("", viewModel.uiState.value.searchKeyword)
        assertEquals(emptyList<BookDraft>(), viewModel.uiState.value.searchResults)
        assertEquals(false, viewModel.uiState.value.hasSubmittedSearch)
        assertEquals(null, viewModel.uiState.value.searchErrorMessage)
    }

    @Test
    fun addSearchResultToWishlist_marksToggleCheckedAndShowsToast() = runTest {
        val repository = FakeRepository()
        val viewModel = BookViewModel(repository, FakeTimeRepository())
        val draft = BookDraft(
            title = "A Wizard of Earthsea",
            author = "Ursula K. Le Guin",
        )

        viewModel.onEvent(BookEvent.AddDraftToWishlist(draft))
        advanceUntilIdle()

        assertEquals(listOf(draft), repository.wishlistDrafts)
        assertEquals(true, draft.searchResultKey() in viewModel.uiState.value.wishlistSearchResultKeys)
        assertEquals("Added to wishlist", viewModel.uiState.value.toastMessage)

        viewModel.onEvent(BookEvent.UncheckDraftWishlist(draft))

        assertEquals(false, draft.searchResultKey() in viewModel.uiState.value.wishlistSearchResultKeys)
    }

    @Test
    fun addSearchResultToReading_marksToggleCheckedAndShowsToast() = runTest {
        val repository = FakeRepository()
        val viewModel = BookViewModel(repository, FakeTimeRepository())
        val draft = BookDraft(
            title = "A Wizard of Earthsea",
            author = "Ursula K. Le Guin",
        )

        viewModel.onEvent(BookEvent.AddDraftToReading(draft))
        advanceUntilIdle()

        assertEquals(listOf(draft), repository.readingDrafts)
        assertEquals(true, draft.searchResultKey() in viewModel.uiState.value.readingSearchResultKeys)
        assertEquals("Added to reading", viewModel.uiState.value.toastMessage)

        viewModel.onEvent(BookEvent.UncheckDraftReading(draft))

        assertEquals(false, draft.searchResultKey() in viewModel.uiState.value.readingSearchResultKeys)
    }

    @Test
    fun stoppingReadingSession_logsTimeEntryAndClearsSession() = runTest {
        val timeRepository = FakeTimeRepository()
        val viewModel = BookViewModel(FakeRepository(), timeRepository)
        val book = sampleBook(BookState.Reading)
        val startTime = Instant.parse("2026-06-17T12:00:00Z")
        val stopTime = Instant.parse("2026-06-17T12:42:00Z")

        viewModel.onEvent(BookEvent.StartReadingSession(book))
        viewModel.onEvent(
            BookEvent.StopReadingSession(
                startTime = startTime,
                stopTime = stopTime,
                stopPage = 96,
                stopProgressPercentage = null,
            )
        )
        advanceUntilIdle()

        val payload = timeRepository.createdEntries.single()
        assertEquals(2, payload.type)
        assertEquals(startTime.toString(), payload.start)
        assertEquals(stopTime.toString(), payload.end)
        assertEquals("Currently reading Page 80 - Page 96", payload.title)
        assertEquals(null, viewModel.uiState.value.readingSession)
    }

    @Test
    fun updatingReadingProgress_logsTimeEntryAndUpdatesBook() = runTest {
        val repository = FakeRepository()
        val timeRepository = FakeTimeRepository()
        val viewModel = BookViewModel(repository, timeRepository)
        val readingBookId = sampleBook(BookState.Reading).id
        val startTime = Instant.parse("2026-06-17T12:00:00Z")
        val stopTime = Instant.parse("2026-06-17T12:10:00Z")

        viewModel.onEvent(BookEvent.OpenDetail(readingBookId))
        advanceUntilIdle()
        viewModel.onEvent(
            BookEvent.UpdateReadingProgress(
                bookId = readingBookId,
                startTime = startTime,
                stopTime = stopTime,
                stopPage = 100,
                stopProgressPercentage = null,
            )
        )
        advanceUntilIdle()

        val payload = timeRepository.createdEntries.single()
        assertEquals(2, payload.type)
        assertEquals(startTime.toString(), payload.start)
        assertEquals(stopTime.toString(), payload.end)
        assertEquals("Currently reading Page 80 - Page 100", payload.title)
        assertEquals(100, repository.updatePayloads.last().currentPage)
    }

    private class FakeRepository : BookRepository(NoopBookApi) {
        val requests = mutableListOf<Pair<BookState, String?>>()
        val wishlistDrafts = mutableListOf<BookDraft>()
        val readingDrafts = mutableListOf<BookDraft>()
        val updatePayloads = mutableListOf<BookUpdateRequest>()

        override fun getBooks(state: BookState, category: String?): Flow<ApiResult<List<Book>>> {
            requests.add(state to category)
            return flowOf(ApiResult.Success(listOf(sampleBook(state))))
        }

        override fun getCategories(): Flow<ApiResult<List<String>>> =
            flowOf(ApiResult.Success(listOf("Science Fiction")))

        override fun getBook(bookId: Long): Flow<ApiResult<Book>> =
            flowOf(ApiResult.Success(sampleBook(BookState.Reading)))

        override fun updateBook(bookId: Long, payload: BookUpdateRequest): Flow<ApiResult<Book>> {
            updatePayloads.add(payload)
            return flowOf(ApiResult.Success(sampleBook(BookState.Reading)))
        }

        override fun addToWishlist(draft: BookDraft): Flow<ApiResult<Book>> {
            wishlistDrafts.add(draft)
            return flowOf(ApiResult.Success(sampleBook(BookState.Wish)))
        }

        override fun addToReading(draft: BookDraft, platform: String): Flow<ApiResult<Book>> {
            readingDrafts.add(draft)
            return flowOf(ApiResult.Success(sampleBook(BookState.Reading)))
        }

        override fun searchBooks(keyword: String): Flow<ApiResult<List<BookDraft>>> =
            flowOf(
                ApiResult.Loading,
                ApiResult.Success(
                    listOf(
                        BookDraft(
                            title = "A Wizard of Earthsea",
                            author = "Ursula K. Le Guin",
                        )
                    )
                )
            )
    }

    private class FakeTimeRepository(
        private val entries: List<TimeEntry> = emptyList(),
    ) : TimeRepository(NoopTimeApi) {
        val createdEntries = mutableListOf<TimeUpsertRequest>()

        override fun getTimeDashboard(
            startDate: LocalDate,
            endDate: LocalDate,
        ): Flow<ApiResult<TimeDashboard>> =
            flowOf(ApiResult.Success(TimeDashboard(statistics = emptyList(), entries = entries)))

        override fun createTimeEntry(payload: TimeUpsertRequest): Flow<ApiResult<Unit>> {
            createdEntries.add(payload)
            return flowOf(ApiResult.Success(Unit))
        }
    }
}

private fun timeEntry(
    type: Int,
    start: OffsetDateTime,
): TimeEntry = TimeEntry(
    id = 1,
    type = type,
    start = start,
    end = start.plusMinutes(30),
    title = "Reading",
    description = null,
)

private object NoopBookApi : BookApi {
    override suspend fun search(keyword: String, limit: Int): JsonElement = JsonArray(emptyList())
    override suspend fun getWishlist(page: Int, size: Int, category: String?): List<BookDto> = emptyList()
    override suspend fun createWishlistBook(payload: BookCreateRequest): BookDto = error("not used")
    override suspend fun getReading(page: Int, size: Int, category: String?): List<BookDto> = emptyList()
    override suspend fun getArchived(page: Int, size: Int, status: String?, category: String?): List<BookDto> = emptyList()
    override suspend fun getCategories(): List<String> = emptyList()
    override suspend fun getBook(bookId: Long): BookDto = error("not used")
    override suspend fun updateBook(bookId: Long, payload: BookUpdateRequest): BookDto = error("not used")
    override suspend fun deleteBook(bookId: Long) = Unit
    override suspend fun startReading(bookId: Long, payload: BookStartReadingRequest): BookDto = error("not used")
    override suspend fun archiveBook(bookId: Long, payload: BookArchiveRequest): BookDto = error("not used")
    override suspend fun getNotes(bookId: Long, page: Int, size: Int): List<BookNoteDto> = emptyList()
    override suspend fun createNote(bookId: Long, payload: BookNoteCreateRequest): BookNoteDto = error("not used")
    override suspend fun updateNote(bookId: Long, noteId: Long, payload: BookNoteUpdateRequest): BookNoteDto =
        error("not used")
    override suspend fun deleteNote(bookId: Long, noteId: Long) = Unit
}

private object NoopTimeApi : TimeApi {
    override suspend fun getStatistics(startDate: String, endDate: String): List<TimeStatisticDto> = emptyList()
    override suspend fun getTimeEntries(startDate: String, endDate: String): List<TimeEntryDto> = emptyList()
    override suspend fun createTimeEntry(payload: TimeUpsertRequest) = Unit
    override suspend fun updateTimeEntry(timeId: Long, payload: TimeUpsertRequest) = Unit
    override suspend fun deleteTimeEntry(timeId: Long) = Unit
    override suspend fun getTimeLabels(type: Int?): List<TimeLabelDto> = emptyList()
    override suspend fun createTimeLabel(payload: TimeLabelCreateRequest): TimeLabelDto = error("not used")
    override suspend fun updateTimeLabel(labelId: Long, payload: TimeLabelUpdateRequest): TimeLabelDto =
        error("not used")

    override suspend fun deleteTimeLabel(labelId: Long) = Unit
}

internal fun sampleBook(state: BookState) = Book(
    id = state.ordinal.toLong() + 1,
    title = when (state) {
        BookState.Reading -> "Currently reading"
        BookState.Wish -> "Next book"
        BookState.Archived -> "Finished book"
    },
    publisher = null,
    author = "Tink",
    coverUrl = null,
    isbn = null,
    description = null,
    rating = null,
    amazonLink = null,
    pages = 200,
    publishYear = 2026,
    state = state,
    platform = "Paper",
    pageFormat = BookPageFormat.Page,
    currentPage = 80,
    progressPercentage = 40.0,
    archiveStatus = null,
    archivedDate = null,
)
