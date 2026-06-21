package app.tinks.tink.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.book.BookPageFormat
import app.tinks.tink.ui.components.AppSnackbarBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeEvent {
    data object Refresh : HomeEvent
    data class CompleteMerriamWords(val count: Int) : HomeEvent
    data object AddWeight : HomeEvent
    data object ToggleReadKeeperSession : HomeEvent
    data object OpenReadKeeperProgress : HomeEvent
    data object DismissReadKeeperProgress : HomeEvent
    data class UpdateReadKeeperProgressInput(val value: String) : HomeEvent
    data object SaveReadKeeperProgress : HomeEvent
}

data class HomeUiState(
    val isRefreshing: Boolean = false,
    val snapshot: HomeSnapshot = HomeSnapshot(),
    val showReadKeeperProgressDialog: Boolean = false,
    val readKeeperProgressInput: String = "",
) {
    val canUpdateReadKeeper: Boolean
        get() = snapshot.readKeeperBook != null
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
) : ViewModel() {
    private val progressDialogState = MutableStateFlow(HomeProgressDialogState())

    val uiState = combine(
        repository.observeSnapshot(),
        progressDialogState,
    ) { snapshot, progressDialog ->
        HomeUiState(
            isRefreshing = progressDialog.isRefreshing,
            snapshot = snapshot,
            showReadKeeperProgressDialog = progressDialog.showReadKeeperProgressDialog,
            readKeeperProgressInput = progressDialog.readKeeperProgressInput,
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
            HomeEvent.ToggleReadKeeperSession -> launchAction {
                if (!repository.toggleReadKeeperSession()) {
                    AppSnackbarBus.showMessage("No reading book cached yet")
                }
            }
            HomeEvent.OpenReadKeeperProgress -> openReadKeeperProgress()
            HomeEvent.DismissReadKeeperProgress -> progressDialogState.update {
                it.copy(showReadKeeperProgressDialog = false, readKeeperProgressInput = "")
            }
            is HomeEvent.UpdateReadKeeperProgressInput -> progressDialogState.update {
                it.copy(readKeeperProgressInput = event.value.filterProgressInput())
            }
            HomeEvent.SaveReadKeeperProgress -> saveReadKeeperProgress()
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
        val book = uiState.value.snapshot.readKeeperBook ?: run {
            AppSnackbarBus.showMessage("No reading book cached yet")
            return
        }
        progressDialogState.update {
            it.copy(
                showReadKeeperProgressDialog = true,
                readKeeperProgressInput = when {
                    book.pageFormat.usesPages -> book.currentPage?.toString().orEmpty()
                    else -> book.progressPercentage?.formatHomeProgressInput(book.pageFormat).orEmpty()
                },
            )
        }
    }

    private fun saveReadKeeperProgress() {
        val book = uiState.value.snapshot.readKeeperBook ?: return
        val rawInput = uiState.value.readKeeperProgressInput
        val page = if (book.pageFormat.usesPages) rawInput.toIntOrNull()?.coerceAtLeast(0) else null
        val progress = if (book.pageFormat.usesPages) {
            null
        } else {
            rawInput.toDoubleOrNull()?.coerceIn(0.0, 100.0)
        }
        if (page == null && progress == null) {
            AppSnackbarBus.showMessage("Enter progress first")
            return
        }

        launchAction {
            if (repository.updateReadKeeperProgress(page, progress)) {
                progressDialogState.update {
                    it.copy(showReadKeeperProgressDialog = false, readKeeperProgressInput = "")
                }
            } else {
                AppSnackbarBus.showMessage("No reading book cached yet")
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
    val showReadKeeperProgressDialog: Boolean = false,
    val readKeeperProgressInput: String = "",
)

private fun String.filterProgressInput(): String =
    filterIndexed { index, char ->
        char.isDigit() || (char == '.' && index > 0 && !take(index).contains('.'))
    }

private fun Double.formatHomeProgressInput(pageFormat: BookPageFormat): String =
    "%.${pageFormat.precision}f".format(this)
