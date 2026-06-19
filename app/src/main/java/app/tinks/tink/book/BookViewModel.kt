package app.tinks.tink.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.network.ApiResult
import app.tinks.tink.time.TimeRepository
import app.tinks.tink.time.TimeUpsertRequest
import app.tinks.tink.ui.components.AppSnackbarBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

sealed interface BooksScreenState : NavKey {
    @Serializable
    data object Home : BooksScreenState

    @Serializable
    data class List(val state: BookState) : BooksScreenState

    @Serializable
    data object Search : BooksScreenState

    @Serializable
    data class SearchResults(val query: String) : BooksScreenState

    @Serializable
    data object YearlySummary : BooksScreenState

    @Serializable
    data object SummaryImage : BooksScreenState

    @Serializable
    data class Detail(val bookId: Long) : BooksScreenState

    @Serializable
    data class Notes(val bookId: Long) : BooksScreenState
}

sealed interface BookEvent {
    data class OpenList(val state: BookState) : BookEvent
    data class OpenArchivedStatus(val status: ArchiveStatus?) : BookEvent
    data object OpenHome : BookEvent
    data object OpenSearch : BookEvent
    data object OpenYearlySummary : BookEvent
    data object OpenSummaryImage : BookEvent
    data class OpenDetail(val bookId: Long) : BookEvent
    data class OpenNotes(val bookId: Long) : BookEvent
    data object NavigateBack : BookEvent
    data object Refresh : BookEvent
    data class SearchKeywordChanged(val keyword: String) : BookEvent
    data object SubmitSearch : BookEvent
    data class SelectSummaryYear(val year: Int) : BookEvent
    data class AddDraftToWishlist(val draft: BookDraft) : BookEvent
    data class AddDraftToReading(val draft: BookDraft) : BookEvent
    data class UncheckDraftWishlist(val draft: BookDraft) : BookEvent
    data class UncheckDraftReading(val draft: BookDraft) : BookEvent
    data class MoveToReading(val book: Book) : BookEvent
    data class SelectCategory(val category: String?) : BookEvent
    data class SelectArchiveStatus(val status: ArchiveStatus?) : BookEvent
    data class Archive(val bookId: Long, val status: ArchiveStatus = ArchiveStatus.Done) : BookEvent
    data class DeleteBook(val bookId: Long) : BookEvent
    data class StartReadingSession(val book: Book) : BookEvent
    data class StopReadingSession(
        val startTime: Instant,
        val stopTime: Instant,
        val stopPage: Int?,
        val stopProgressPercentage: Double?,
    ) : BookEvent
    data class SaveBook(
        val bookId: Long,
        val title: String,
        val platform: String?,
        val category: String?,
        val pages: Int?,
        val pageFormat: BookPageFormat,
        val currentPage: Int?,
        val progressPercentage: Double?,
    ) : BookEvent
    data class UpdateReadingProgress(
        val bookId: Long,
        val startTime: Instant,
        val stopTime: Instant,
        val stopPage: Int?,
        val stopProgressPercentage: Double?,
    ) : BookEvent
    data class SaveNote(
        val noteId: Long?,
        val content: String,
        val page: Int?,
        val progressPercentage: Double?,
    ) : BookEvent
    data class DeleteNote(val noteId: Long) : BookEvent
}

data class ReadingSessionState(
    val bookId: Long,
    val bookTitle: String,
    val pageFormat: BookPageFormat,
    val startTime: Instant,
    val startPage: Int?,
    val startProgressPercentage: Double?,
    val startProgressLabel: String,
)

data class BookUiState(
    val isLoading: Boolean = false,
    val screen: BooksScreenState = BooksScreenState.Home,
    val readingBooks: List<Book> = emptyList(),
    val wishlistBooks: List<Book> = emptyList(),
    val archivedBooks: List<Book> = emptyList(),
    val selectedBook: Book? = null,
    val notes: List<BookNote> = emptyList(),
    val searchKeyword: String = "",
    val searchResults: List<BookDraft> = emptyList(),
    val hasSubmittedSearch: Boolean = false,
    val searchErrorMessage: String? = null,
    val wishlistSearchResultKeys: Set<String> = emptySet(),
    val readingSearchResultKeys: Set<String> = emptySet(),
    val toastMessage: String? = null,
    val toastId: Long = 0,
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val selectedArchiveStatus: ArchiveStatus? = null,
    val selectedSummaryYear: Int? = null,
    val navigationStack: List<BooksScreenState> = listOf(BooksScreenState.Home),
    val readingSession: ReadingSessionState? = null,
    val readingRecordDates: Set<LocalDate> = emptySet(),
)

@HiltViewModel
class BookViewModel @Inject constructor(
    private val repository: BookRepository,
    private val timeRepository: TimeRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BookUiState())
    val uiState = _state.map { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value)

    private var searchJob: Job? = null
    private var mutationJob: Job? = null

    init {
        loadCategories()
        refreshAll()
    }

    fun onEvent(event: BookEvent) {
        when (event) {
            BookEvent.OpenHome -> {
                _state.update {
                    it.copy(
                        screen = BooksScreenState.Home,
                        navigationStack = listOf(BooksScreenState.Home),
                        selectedCategory = null,
                        selectedArchiveStatus = null,
                    )
                }
                refreshAll()
            }
            is BookEvent.OpenList -> {
                _state.update { it.copy(selectedCategory = null, selectedArchiveStatus = null) }
                navigateTo(BooksScreenState.List(event.state))
                loadList(event.state, null)
            }
            is BookEvent.OpenArchivedStatus -> {
                _state.update { it.copy(selectedCategory = null, selectedArchiveStatus = event.status) }
                navigateTo(BooksScreenState.List(BookState.Archived))
                loadList(BookState.Archived, null)
            }
            BookEvent.OpenSearch -> navigateTo(BooksScreenState.Search)
            BookEvent.OpenYearlySummary -> navigateTo(BooksScreenState.YearlySummary)
            BookEvent.OpenSummaryImage -> navigateTo(BooksScreenState.SummaryImage)
            is BookEvent.OpenDetail -> {
                navigateTo(BooksScreenState.Detail(event.bookId))
                openDetail(event.bookId)
            }
            is BookEvent.OpenNotes -> {
                navigateTo(BooksScreenState.Notes(event.bookId))
                openNotes(event.bookId)
            }
            BookEvent.NavigateBack -> {
                when (_state.value.screen) {
                    BooksScreenState.Search,
                    is BooksScreenState.SearchResults -> {
                        closeSearch()
                        refreshAll()
                    }
                    else -> {
                        navigateBack()
                        refreshCurrent()
                    }
                }
            }
            BookEvent.Refresh -> refreshCurrent()
            is BookEvent.SearchKeywordChanged -> _state.update { it.copy(searchKeyword = event.keyword) }
            BookEvent.SubmitSearch -> search()
            is BookEvent.SelectSummaryYear -> _state.update { it.copy(selectedSummaryYear = event.year) }
            is BookEvent.AddDraftToWishlist -> addDraftToWishlist(event.draft)
            is BookEvent.AddDraftToReading -> addDraftToReading(event.draft)
            is BookEvent.UncheckDraftWishlist -> uncheckDraftWishlist(event.draft)
            is BookEvent.UncheckDraftReading -> uncheckDraftReading(event.draft)
            is BookEvent.MoveToReading -> moveToReading(event.book)
            is BookEvent.SelectCategory -> selectCategory(event.category)
            is BookEvent.SelectArchiveStatus -> _state.update { it.copy(selectedArchiveStatus = event.status) }
            is BookEvent.Archive -> archive(event.bookId, event.status)
            is BookEvent.DeleteBook -> deleteBook(event.bookId)
            is BookEvent.StartReadingSession -> startReadingSession(event.book)
            is BookEvent.StopReadingSession -> stopReadingSession(event)
            is BookEvent.SaveBook -> saveBook(event)
            is BookEvent.UpdateReadingProgress -> updateReadingProgress(event)
            is BookEvent.SaveNote -> saveNote(event)
            is BookEvent.DeleteNote -> deleteNote(event.noteId)
        }
    }

    private fun refreshCurrent() {
        when (val screen = _state.value.screen) {
            BooksScreenState.Home -> refreshAll()
            is BooksScreenState.List -> loadList(screen.state, _state.value.selectedCategory)
            BooksScreenState.Search -> search()
            is BooksScreenState.SearchResults -> search(screen.query)
            BooksScreenState.YearlySummary -> loadList(BookState.Archived, _state.value.selectedCategory)
            BooksScreenState.SummaryImage -> loadList(BookState.Archived, _state.value.selectedCategory)
            is BooksScreenState.Detail -> openDetail(screen.bookId)
            is BooksScreenState.Notes -> openNotes(screen.bookId)
        }
    }

    private fun refreshAll() {
        loadList(BookState.Reading, selectedCategoryFor(BookState.Reading))
        loadList(BookState.Wish, selectedCategoryFor(BookState.Wish))
        loadList(BookState.Archived, selectedCategoryFor(BookState.Archived))
        loadReadingRecordDates()
    }

    private fun loadReadingRecordDates() {
        val startDate = LocalDate.now().withDayOfYear(1)
        val endDate = startDate.withDayOfYear(startDate.lengthOfYear())
        timeRepository.getTimeDashboard(startDate, endDate)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> Unit
                    is ApiResult.Success -> _state.update {
                        it.copy(
                            readingRecordDates = result.data.entries
                                .asSequence()
                                .filter { entry -> entry.type == READING_TIME_TYPE }
                                .map { entry -> entry.start.toLocalDate() }
                                .filter { date -> date.year == startDate.year }
                                .toSet()
                        )
                    }
                    is ApiResult.Error -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    private fun selectedCategoryFor(state: BookState): String? {
        val visibleList = _state.value.screen as? BooksScreenState.List ?: return null
        return _state.value.selectedCategory.takeIf { visibleList.state == state }
    }

    private fun loadList(state: BookState, category: String? = null) {
        repository.getBooks(state, category)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is ApiResult.Success -> _state.update {
                        when (state) {
                            BookState.Reading -> it.copy(readingBooks = result.data, isLoading = false)
                            BookState.Wish -> it.copy(wishlistBooks = result.data, isLoading = false)
                            BookState.Archived -> it.copy(archivedBooks = result.data, isLoading = false)
                        }
                    }
                    is ApiResult.Error -> failLoading { loadList(state, category) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadCategories() {
        repository.getCategories()
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> Unit
                    is ApiResult.Success -> _state.update { it.copy(categories = result.data) }
                    is ApiResult.Error -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    private fun selectCategory(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
        val state = (_state.value.screen as? BooksScreenState.List)?.state ?: return
        loadList(state, category)
    }

    private fun openDetail(bookId: Long) {
        val cachedBook = _state.value.findBook(bookId)
        loadDetailNotes(bookId)
        repository.getBook(bookId)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> _state.update {
                        it.copy(
                            isLoading = true,
                            screen = BooksScreenState.Detail(bookId),
                            selectedBook = cachedBook ?: it.selectedBook?.takeIf { book -> book.id == bookId },
                            notes = emptyList(),
                        )
                    }
                    is ApiResult.Success -> _state.update {
                        it.copy(
                            selectedBook = result.data,
                            screen = BooksScreenState.Detail(bookId),
                            isLoading = false,
                        )
                    }
                    is ApiResult.Error -> failLoading { openDetail(bookId) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadDetailNotes(bookId: Long) {
        repository.getNotes(bookId)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> _state.update { it.copy(notes = emptyList()) }
                    is ApiResult.Success -> _state.update { it.copy(notes = result.data) }
                    is ApiResult.Error -> _state.update { it.copy(notes = emptyList()) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun openNotes(bookId: Long) {
        repository.getNotes(bookId)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> _state.update {
                        it.copy(isLoading = true, screen = BooksScreenState.Notes(bookId))
                    }
                    is ApiResult.Success -> _state.update {
                        it.copy(notes = result.data, screen = BooksScreenState.Notes(bookId), isLoading = false)
                    }
                    is ApiResult.Error -> failLoading { openNotes(bookId) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun search(keywordOverride: String? = null) {
        val keyword = (keywordOverride ?: _state.value.searchKeyword).trim()
        if (keyword.isEmpty()) return
        searchJob?.cancel()
        searchJob = repository.searchBooks(keyword)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> _state.update {
                        it.copy(
                            isLoading = true,
                            searchResults = emptyList(),
                            hasSubmittedSearch = true,
                            searchErrorMessage = null,
                        )
                    }
                    is ApiResult.Success -> _state.update {
                        it.copy(
                            searchResults = result.data,
                            isLoading = false,
                            screen = BooksScreenState.SearchResults(keyword),
                            navigationStack = it.navigationStack.pushOrReplaceSearchResults(BooksScreenState.SearchResults(keyword)),
                            hasSubmittedSearch = true,
                            searchErrorMessage = null,
                        )
                    }
                    is ApiResult.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                searchResults = emptyList(),
                                screen = BooksScreenState.SearchResults(keyword),
                                navigationStack = it.navigationStack.pushOrReplaceSearchResults(BooksScreenState.SearchResults(keyword)),
                                hasSubmittedSearch = true,
                                searchErrorMessage = result.message,
                            )
                        }
                        AppSnackbarBus.showApiFailure(onRetry = ::search)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun addDraftToWishlist(draft: BookDraft) {
        mutate({ repository.addToWishlist(draft) }) {
            val key = draft.searchResultKey()
            _state.update {
                it.copy(
                    wishlistSearchResultKeys = it.wishlistSearchResultKeys + key,
                    toastMessage = "Added to wishlist",
                    toastId = it.toastId + 1,
                )
            }
            refreshAll()
        }
    }

    private fun addDraftToReading(draft: BookDraft) {
        mutate({ repository.addToReading(draft) }) {
            val key = draft.searchResultKey()
            _state.update {
                it.copy(
                    readingSearchResultKeys = it.readingSearchResultKeys + key,
                    toastMessage = "Added to reading",
                    toastId = it.toastId + 1,
                )
            }
            refreshAll()
        }
    }

    private fun uncheckDraftWishlist(draft: BookDraft) {
        val key = draft.searchResultKey()
        _state.update { it.copy(wishlistSearchResultKeys = it.wishlistSearchResultKeys - key) }
    }

    private fun uncheckDraftReading(draft: BookDraft) {
        val key = draft.searchResultKey()
        _state.update { it.copy(readingSearchResultKeys = it.readingSearchResultKeys - key) }
    }

    private fun moveToReading(book: Book) {
        mutate({
            repository.moveToReading(
                bookId = book.id,
                platform = book.platform ?: "General",
                pageFormat = book.pageFormat,
                currentPage = book.currentPage ?: 0,
                progressPercentage = book.progressPercentage ?: 0.0,
            )
        }) {
            refreshAll()
        }
    }

    private fun archive(bookId: Long, status: ArchiveStatus) {
        mutate({ repository.archiveBook(bookId, status) }) {
            refreshAll()
            _state.update {
                it.copy(
                    screen = BooksScreenState.Home,
                    navigationStack = listOf(BooksScreenState.Home),
                )
            }
        }
    }

    private fun deleteBook(bookId: Long) {
        mutationJob?.cancel()
        mutationJob = repository.deleteBook(bookId)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is ApiResult.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                screen = BooksScreenState.Home,
                                navigationStack = listOf(BooksScreenState.Home),
                            )
                        }
                        refreshAll()
                    }
                    is ApiResult.Error -> failLoading { deleteBook(bookId) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startReadingSession(book: Book) {
        _state.update {
            it.copy(
                readingSession = ReadingSessionState(
                    bookId = book.id,
                    bookTitle = book.title,
                    pageFormat = book.pageFormat,
                    startTime = Instant.now(),
                    startPage = book.currentPage,
                    startProgressPercentage = book.progressPercentage,
                    startProgressLabel = book.readingSessionProgressLabel(),
                )
            )
        }
    }

    private fun stopReadingSession(event: BookEvent.StopReadingSession) {
        val session = _state.value.readingSession ?: return
        val startTime = event.startTime
        val stopTime = if (event.stopTime.isAfter(startTime)) {
            event.stopTime
        } else {
            startTime.plusSeconds(1)
        }
        val stopProgressLabel = session.stopProgressLabel(event.stopPage, event.stopProgressPercentage)
        val payload = TimeUpsertRequest(
            type = READING_TIME_TYPE,
            start = startTime.toString(),
            end = stopTime.toString(),
            title = "${session.bookTitle} ${session.startProgressLabel} - $stopProgressLabel",
            description = "ReadKeeper reading session",
        )
        mutate({
            timeRepository.createTimeEntry(payload)
        }) {
            _state.update { it.copy(readingSession = null) }
            repository.updateBook(
                session.bookId,
                BookUpdateRequest(
                    currentPage = if (session.pageFormat.usesPages) event.stopPage else null,
                    progressPercentage = if (session.pageFormat.usesPages) null else event.stopProgressPercentage,
                )
            ).launchIn(viewModelScope)
            openDetail(session.bookId)
            refreshAll()
        }
    }

    private fun saveBook(event: BookEvent.SaveBook) {
        val currentBook = _state.value.selectedBook?.takeIf { it.id == event.bookId }
        mutate({
            repository.updateBook(
                event.bookId,
                BookUpdateRequest(
                    title = event.title,
                    platform = event.platform,
                    category = event.category,
                    pages = event.pages.takeIf { it != currentBook?.pages },
                    pageFormat = event.pageFormat
                        .takeIf { it != currentBook?.pageFormat }
                        ?.wireValue,
                    currentPage = event.currentPage,
                    progressPercentage = event.progressPercentage,
                )
            )
        }) {
            openDetail(event.bookId)
            loadCategories()
            refreshAll()
        }
    }

    private fun updateReadingProgress(event: BookEvent.UpdateReadingProgress) {
        val currentBook = _state.value.selectedBook?.takeIf { it.id == event.bookId }
            ?: _state.value.findBook(event.bookId)
            ?: return
        val startTime = event.startTime
        val stopTime = if (event.stopTime.isAfter(startTime)) {
            event.stopTime
        } else {
            startTime.plusSeconds(1)
        }
        val stopProgressLabel = currentBook.progressLabel(event.stopPage, event.stopProgressPercentage)
        val payload = TimeUpsertRequest(
            type = READING_TIME_TYPE,
            start = startTime.toString(),
            end = stopTime.toString(),
            title = "${currentBook.title} ${currentBook.readingSessionProgressLabel()} - $stopProgressLabel",
            description = "ReadKeeper reading progress update",
        )
        mutate({
            timeRepository.createTimeEntry(payload)
        }) {
            repository.updateBook(
                event.bookId,
                BookUpdateRequest(
                    currentPage = if (currentBook.pageFormat.usesPages) event.stopPage else null,
                    progressPercentage = if (currentBook.pageFormat.usesPages) null else event.stopProgressPercentage,
                )
            ).launchIn(viewModelScope)
            openDetail(event.bookId)
            refreshAll()
        }
    }

    private fun saveNote(event: BookEvent.SaveNote) {
        val bookId = (_state.value.screen as? BooksScreenState.Notes)?.bookId
            ?: _state.value.selectedBook?.id
            ?: return
        mutate({
            repository.saveNote(
                bookId = bookId,
                noteId = event.noteId,
                content = event.content,
                page = event.page,
                progressPercentage = event.progressPercentage,
            )
        }) {
            openNotes(bookId)
            if (event.page != null || event.progressPercentage != null) {
                repository.updateBook(
                    bookId,
                    BookUpdateRequest(
                        currentPage = event.page,
                        progressPercentage = event.progressPercentage,
                    )
                ).launchIn(viewModelScope)
            }
        }
    }

    private fun deleteNote(noteId: Long) {
        val bookId = (_state.value.screen as? BooksScreenState.Notes)?.bookId ?: return
        mutationJob?.cancel()
        mutationJob = repository.deleteNote(bookId, noteId)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is ApiResult.Success -> openNotes(bookId)
                    is ApiResult.Error -> failLoading { deleteNote(noteId) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun <T> mutate(
        call: () -> kotlinx.coroutines.flow.Flow<ApiResult<T>>,
        onSuccess: () -> Unit,
    ) {
        mutationJob?.cancel()
        mutationJob = call()
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is ApiResult.Success -> {
                        _state.update { it.copy(isLoading = false) }
                        onSuccess()
                    }
                    is ApiResult.Error -> failLoading { mutate(call, onSuccess) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun failLoading(onRetry: () -> Unit) {
        _state.update { it.copy(isLoading = false) }
        AppSnackbarBus.showApiFailure(onRetry = onRetry)
    }

    private fun navigateTo(screen: BooksScreenState) {
        _state.update {
            if (it.navigationStack.lastOrNull() == screen) {
                return@update it.copy(screen = screen)
            }
            it.copy(
                screen = screen,
                navigationStack = it.navigationStack + screen,
            )
        }
    }

    private fun navigateBack() {
        _state.update {
            val stack = it.navigationStack.dropLast(1).ifEmpty { listOf(BooksScreenState.Home) }
            it.copy(screen = stack.last(), navigationStack = stack)
        }
    }

    private fun closeSearch() {
        searchJob?.cancel()
        _state.update {
            it.copy(
                screen = BooksScreenState.Home,
                navigationStack = listOf(BooksScreenState.Home),
                searchKeyword = "",
                searchResults = emptyList(),
                hasSubmittedSearch = false,
                searchErrorMessage = null,
                wishlistSearchResultKeys = emptySet(),
                readingSearchResultKeys = emptySet(),
                isLoading = false,
            )
        }
    }
}

private fun BookUiState.findBook(bookId: Long): Book? =
    readingBooks.firstOrNull { it.id == bookId }
        ?: wishlistBooks.firstOrNull { it.id == bookId }
        ?: archivedBooks.firstOrNull { it.id == bookId }

private const val READING_TIME_TYPE = 2

private fun Book.readingSessionProgressLabel(): String =
    if (pageFormat.usesPages) {
        currentPage?.let { "Page $it" } ?: "Page --"
    } else {
        progressPercentage?.formatSessionPercent(pageFormat) ?: "--%"
    }

private fun Book.progressLabel(
    page: Int?,
    progressPercentage: Double?,
): String =
    if (pageFormat.usesPages) {
        page?.let { "Page $it" } ?: "Page --"
    } else {
        progressPercentage?.formatSessionPercent(pageFormat) ?: "--%"
    }

private fun ReadingSessionState.stopProgressLabel(
    stopPage: Int?,
    stopProgressPercentage: Double?,
): String =
    if (pageFormat.usesPages) {
        stopPage?.let { "Page $it" } ?: "Page --"
    } else {
        stopProgressPercentage?.formatSessionPercent(pageFormat) ?: "--%"
    }

private fun Double.formatSessionPercent(format: BookPageFormat): String =
    if (format.precision == 0) {
        "${toInt()}%"
    } else {
        String.format(Locale.US, "%.${format.precision}f%%", this)
    }

private fun List<BooksScreenState>.pushOrReplaceSearchResults(
    screen: BooksScreenState.SearchResults,
): List<BooksScreenState> =
    when {
        lastOrNull() is BooksScreenState.SearchResults -> dropLast(1) + screen
        lastOrNull() == screen -> this
        else -> this + screen
    }

internal fun BookDraft.searchResultKey(): String =
    listOf(
        isbn.orEmpty(),
        title,
        author.orEmpty(),
        publisher.orEmpty(),
        publishYear?.toString().orEmpty(),
    ).joinToString("|") { it.trim().lowercase(Locale.US) }
