package app.tinks.tink.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.network.ApiResult
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
import javax.inject.Inject

sealed interface BooksScreenState {
    data object Home : BooksScreenState
    data class List(val state: BookState) : BooksScreenState
    data object Search : BooksScreenState
    data class Detail(val bookId: Long) : BooksScreenState
    data class Notes(val bookId: Long) : BooksScreenState
}

sealed interface BookEvent {
    data class OpenList(val state: BookState) : BookEvent
    data object OpenHome : BookEvent
    data object OpenSearch : BookEvent
    data class OpenDetail(val bookId: Long) : BookEvent
    data class OpenNotes(val bookId: Long) : BookEvent
    data object BackToHome : BookEvent
    data object Refresh : BookEvent
    data class SearchKeywordChanged(val keyword: String) : BookEvent
    data object SubmitSearch : BookEvent
    data class AddDraftToWishlist(val draft: BookDraft) : BookEvent
    data class AddDraftToReading(val draft: BookDraft) : BookEvent
    data class MoveToReading(val book: Book) : BookEvent
    data class Archive(val bookId: Long, val status: ArchiveStatus = ArchiveStatus.Done) : BookEvent
    data class DeleteBook(val bookId: Long) : BookEvent
    data class SaveBook(
        val bookId: Long,
        val title: String,
        val platform: String?,
        val currentPage: Int?,
        val progressPercentage: Double?,
    ) : BookEvent
    data class SaveNote(
        val noteId: Long?,
        val content: String,
        val page: Int?,
        val progressPercentage: Double?,
    ) : BookEvent
    data class DeleteNote(val noteId: Long) : BookEvent
}

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
)

@HiltViewModel
class BookViewModel @Inject constructor(
    private val repository: BookRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BookUiState())
    val uiState = _state.map { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value)

    private var searchJob: Job? = null
    private var mutationJob: Job? = null

    init {
        refreshAll()
    }

    fun onEvent(event: BookEvent) {
        when (event) {
            BookEvent.OpenHome -> {
                _state.update { it.copy(screen = BooksScreenState.Home) }
                refreshAll()
            }
            is BookEvent.OpenList -> {
                _state.update { it.copy(screen = BooksScreenState.List(event.state)) }
                loadList(event.state)
            }
            BookEvent.OpenSearch -> _state.update { it.copy(screen = BooksScreenState.Search) }
            is BookEvent.OpenDetail -> openDetail(event.bookId)
            is BookEvent.OpenNotes -> openNotes(event.bookId)
            BookEvent.BackToHome -> _state.update { it.copy(screen = BooksScreenState.Home) }
            BookEvent.Refresh -> refreshCurrent()
            is BookEvent.SearchKeywordChanged -> _state.update { it.copy(searchKeyword = event.keyword) }
            BookEvent.SubmitSearch -> search()
            is BookEvent.AddDraftToWishlist -> addDraftToWishlist(event.draft)
            is BookEvent.AddDraftToReading -> addDraftToReading(event.draft)
            is BookEvent.MoveToReading -> moveToReading(event.book)
            is BookEvent.Archive -> archive(event.bookId, event.status)
            is BookEvent.DeleteBook -> deleteBook(event.bookId)
            is BookEvent.SaveBook -> saveBook(event)
            is BookEvent.SaveNote -> saveNote(event)
            is BookEvent.DeleteNote -> deleteNote(event.noteId)
        }
    }

    private fun refreshCurrent() {
        when (val screen = _state.value.screen) {
            BooksScreenState.Home -> refreshAll()
            is BooksScreenState.List -> loadList(screen.state)
            BooksScreenState.Search -> search()
            is BooksScreenState.Detail -> openDetail(screen.bookId)
            is BooksScreenState.Notes -> openNotes(screen.bookId)
        }
    }

    private fun refreshAll() {
        loadList(BookState.Reading)
        loadList(BookState.Wish)
        loadList(BookState.Archived)
    }

    private fun loadList(state: BookState) {
        repository.getBooks(state)
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
                    is ApiResult.Error -> failLoading { loadList(state) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun openDetail(bookId: Long) {
        repository.getBook(bookId)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> _state.update {
                        it.copy(isLoading = true, screen = BooksScreenState.Detail(bookId))
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

    private fun search() {
        val keyword = _state.value.searchKeyword.trim()
        if (keyword.isEmpty()) return
        searchJob?.cancel()
        searchJob = repository.searchGoogleBooks(keyword)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> _state.update { it.copy(isLoading = true, searchResults = emptyList()) }
                    is ApiResult.Success -> _state.update {
                        it.copy(searchResults = result.data, isLoading = false)
                    }
                    is ApiResult.Error -> failLoading(::search)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun addDraftToWishlist(draft: BookDraft) {
        mutate({ repository.addToWishlist(draft) }) { refreshAll() }
    }

    private fun addDraftToReading(draft: BookDraft) {
        mutate({ repository.addToReading(draft) }) { refreshAll() }
    }

    private fun moveToReading(book: Book) {
        mutate({
            repository.moveToReading(
                bookId = book.id,
                platform = book.platform ?: "General",
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
            _state.update { it.copy(screen = BooksScreenState.Home) }
        }
    }

    private fun deleteBook(bookId: Long) {
        mutationJob?.cancel()
        mutationJob = repository.deleteBook(bookId)
            .onEach { result ->
                when (result) {
                    ApiResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is ApiResult.Success -> {
                        _state.update { it.copy(isLoading = false, screen = BooksScreenState.Home) }
                        refreshAll()
                    }
                    is ApiResult.Error -> failLoading { deleteBook(bookId) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun saveBook(event: BookEvent.SaveBook) {
        mutate({
            repository.updateBook(
                event.bookId,
                BookUpdateRequest(
                    title = event.title,
                    platform = event.platform,
                    currentPage = event.currentPage,
                    progressPercentage = event.progressPercentage,
                )
            )
        }) {
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
}
