package app.tinks.tink.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.book.Book
import app.tinks.tink.book.BookEvent
import app.tinks.tink.book.READING_TIME_TYPE
import app.tinks.tink.network.ApiResult
import app.tinks.tink.time.TimeEntry
import app.tinks.tink.time.TimeRepository
import app.tinks.tink.ui.components.AppSnackbarBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface HomeEvent {
    data object Refresh : HomeEvent
    data class CompleteMerriamWords(val count: Int) : HomeEvent
    data object AddWeight : HomeEvent
    data object OpenReadKeeperProgress : HomeEvent
    data object DismissReadKeeperProgress : HomeEvent
    data class SaveReadKeeperProgress(val update: BookEvent.UpdateReadingProgress) : HomeEvent
}

data class HomeUiState(
    val isRefreshing: Boolean = false,
    val snapshot: HomeSnapshot = HomeSnapshot(),
    val readKeeperProgressBook: Book? = null,
    val readingTimeEntries: List<TimeEntry> = emptyList(),
) {
    val canUpdateReadKeeper: Boolean
        get() = snapshot.readKeeperBook != null
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val timeRepository: TimeRepository,
) : ViewModel() {
    private val progressDialogState = MutableStateFlow(HomeProgressDialogState())
    private var openProgressJob: Job? = null
    private var saveProgressJob: Job? = null

    val uiState = combine(
        repository.observeSnapshot(),
        progressDialogState,
    ) { snapshot, progressDialog ->
        HomeUiState(
            isRefreshing = progressDialog.isRefreshing,
            snapshot = snapshot,
            readKeeperProgressBook = progressDialog.readKeeperProgressBook,
            readingTimeEntries = progressDialog.readingTimeEntries,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    init {
        refresh()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Refresh -> refresh()
            is HomeEvent.CompleteMerriamWords -> launchAction {
                if (!repository.completeMerriamWords(event.count)) {
                    AppSnackbarBus.showMessage("No M-W status cached yet")
                }
            }
            HomeEvent.AddWeight -> launchAction {
                if (!repository.addLatestWeight()) {
                    AppSnackbarBus.showMessage("No cached weight yet")
                }
            }
            HomeEvent.OpenReadKeeperProgress -> openReadKeeperProgress()
            HomeEvent.DismissReadKeeperProgress -> dismissReadKeeperProgress()
            is HomeEvent.SaveReadKeeperProgress -> saveReadKeeperProgress(event.update)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            progressDialogState.update { it.copy(isRefreshing = true) }
            runCatching { repository.refreshHome() }
            progressDialogState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun openReadKeeperProgress() {
        val bookId = uiState.value.snapshot.readKeeperBook?.id ?: return
        openProgressJob?.cancel()
        openProgressJob = viewModelScope.launch {
            val book = repository.getCachedReadKeeperBook(bookId) ?: run {
                AppSnackbarBus.showMessage("No reading book cached yet")
                return@launch
            }
            progressDialogState.update { it.copy(readKeeperProgressBook = book, readingTimeEntries = emptyList()) }
            val yearStart = LocalDate.now().withDayOfYear(1)
            timeRepository.getTimeDashboard(yearStart, yearStart.withDayOfYear(yearStart.lengthOfYear()))
                .collect { result ->
                    if (result is ApiResult.Success) {
                        val entries = result.data.entries.filter { it.type == READING_TIME_TYPE }
                        progressDialogState.update { it.copy(readingTimeEntries = entries) }
                    }
                }
        }
    }

    private fun dismissReadKeeperProgress() {
        openProgressJob?.cancel()
        progressDialogState.update { it.copy(readKeeperProgressBook = null, readingTimeEntries = emptyList()) }
    }

    private fun saveReadKeeperProgress(event: BookEvent.UpdateReadingProgress) {
        val book = progressDialogState.value.readKeeperProgressBook?.takeIf { it.id == event.bookId } ?: return
        if (saveProgressJob?.isActive == true) return
        saveProgressJob = viewModelScope.launch {
            val saved = try {
                repository.updateReadKeeperProgress(book, event)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            if (saved) {
                if (progressDialogState.value.readKeeperProgressBook == book) dismissReadKeeperProgress()
            } else {
                AppSnackbarBus.showMessage("Could not save reading progress")
            }
        }
    }

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { AppSnackbarBus.showMessage("Saved locally; will retry when online") }
        }
    }
}

private data class HomeProgressDialogState(
    val isRefreshing: Boolean = false,
    val readKeeperProgressBook: Book? = null,
    val readingTimeEntries: List<TimeEntry> = emptyList(),
)
