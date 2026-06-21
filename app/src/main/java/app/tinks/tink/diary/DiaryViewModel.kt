package app.tinks.tink.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.ui.components.AppSnackbarBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year
import javax.inject.Inject

sealed interface DiaryEvent {
    data object RefreshWeek : DiaryEvent
    data object OpenList : DiaryEvent
    data class OpenDetails(val id: String) : DiaryEvent
    data object ComposeNew : DiaryEvent
    data class ComposeExisting(val id: String) : DiaryEvent
    data object NavigateBack : DiaryEvent
    data class SearchChanged(val query: String) : DiaryEvent
    data class TitleChanged(val value: String) : DiaryEvent
    data class ContentChanged(val value: String) : DiaryEvent
    data class TypeChanged(val value: DiaryType) : DiaryEvent
    data class StartDateChanged(val value: LocalDate) : DiaryEvent
    data class EndDateChanged(val value: LocalDate) : DiaryEvent
    data object SaveDraft : DiaryEvent
    data object SaveDiary : DiaryEvent
    data class DeleteDiary(val id: String) : DiaryEvent
    data class SyncToTime(val id: String) : DiaryEvent
    data class RemoveTimeEvent(val id: String) : DiaryEvent
    data class UpdateWeekOffset(val offset: Int) : DiaryEvent
}

sealed interface DiaryScreenState {
    data object Home : DiaryScreenState
    data object List : DiaryScreenState
    data class Details(val id: String) : DiaryScreenState
    data class Compose(val id: String?) : DiaryScreenState
}

data class DiaryUiState(
    val screen: DiaryScreenState = DiaryScreenState.Home,
    val diaries: List<Diary> = emptyList(),
    val recentDiaries: List<Diary> = emptyList(),
    val drafts: List<Diary> = emptyList(),
    val weeklyRecordData: WeeklyRecordData = WeeklyRecordData(null, emptyList()),
    val contributionYear: Int = Year.now().value,
    val searchQuery: String = "",
    val draft: Diary = Diary(),
    val composeOriginalId: String? = null,
    val isSaving: Boolean = false,
    val syncingIds: Set<String> = emptySet(),
) {
    val selectedDiary: Diary?
        get() = (screen as? DiaryScreenState.Details)?.id?.let { id ->
            diaries.firstOrNull { it.id == id }
        }

    val filteredDiaries: List<Diary>
        get() = if (searchQuery.isBlank()) {
            diaries
        } else {
            val query = searchQuery.trim()
            diaries.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
            }
        }

    val contributionDates: Set<LocalDate>
        get() = diaries.flatMap { it.contributionDatesForYear(contributionYear) }.toSet()
}

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DiaryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DiaryUiState())
    val uiState = _state.map { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value)

    private var weeklyJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getAllDiaries().collectLatest { diaries ->
                _state.update { it.copy(diaries = diaries) }
            }
        }
        viewModelScope.launch {
            repository.getRecentDiaries().collectLatest { diaries ->
                _state.update { it.copy(recentDiaries = diaries) }
            }
        }
        viewModelScope.launch {
            repository.getAllDrafts().collectLatest { drafts ->
                _state.update { it.copy(drafts = drafts) }
            }
        }
        updateWeeklyRecord(0)
    }

    fun onEvent(event: DiaryEvent) {
        when (event) {
            DiaryEvent.RefreshWeek -> updateWeeklyRecord(0)
            DiaryEvent.OpenList -> _state.update { it.copy(screen = DiaryScreenState.List) }
            is DiaryEvent.OpenDetails -> _state.update { it.copy(screen = DiaryScreenState.Details(event.id)) }
            DiaryEvent.ComposeNew -> openCompose(null)
            is DiaryEvent.ComposeExisting -> openCompose(event.id)
            DiaryEvent.NavigateBack -> navigateBack()
            is DiaryEvent.SearchChanged -> _state.update { it.copy(searchQuery = event.query) }
            is DiaryEvent.TitleChanged -> _state.update { it.copy(draft = it.draft.copy(title = event.value)) }
            is DiaryEvent.ContentChanged -> _state.update { it.copy(draft = it.draft.copy(content = event.value)) }
            is DiaryEvent.TypeChanged -> updateDraftType(event.value)
            is DiaryEvent.StartDateChanged -> updateDraftDates(start = event.value, end = null)
            is DiaryEvent.EndDateChanged -> updateDraftDates(start = null, end = event.value)
            DiaryEvent.SaveDraft -> saveDraft()
            DiaryEvent.SaveDiary -> saveDiary()
            is DiaryEvent.DeleteDiary -> deleteDiary(event.id)
            is DiaryEvent.SyncToTime -> syncToTime(event.id)
            is DiaryEvent.RemoveTimeEvent -> removeTimeEvent(event.id)
            is DiaryEvent.UpdateWeekOffset -> updateWeeklyRecord(event.offset)
        }
    }

    private fun openCompose(id: String?) {
        viewModelScope.launch {
            val draft = id?.let { repository.getDraft(it) ?: repository.getDiary(it) } ?: Diary()
            _state.update {
                it.copy(
                    screen = DiaryScreenState.Compose(id),
                    draft = draft,
                    composeOriginalId = id,
                )
            }
        }
    }

    private fun navigateBack() {
        _state.update { state ->
            val next = when (state.screen) {
                DiaryScreenState.Home -> DiaryScreenState.Home
                DiaryScreenState.List -> DiaryScreenState.Home
                is DiaryScreenState.Details -> DiaryScreenState.Home
                is DiaryScreenState.Compose -> state.composeOriginalId?.let { DiaryScreenState.Details(it) }
                    ?: DiaryScreenState.Home
            }
            state.copy(screen = next, isSaving = false)
        }
    }

    private fun updateDraftType(type: DiaryType) {
        _state.update { state ->
            val (start, end) = dateRangeForType(type, state.draft.startDate)
            state.copy(draft = state.draft.copy(type = type, startDate = start, endDate = end))
        }
    }

    private fun updateDraftDates(start: LocalDate?, end: LocalDate?) {
        _state.update { state ->
            val draft = state.draft
            val nextStart = start ?: draft.startDate
            val nextEnd = end ?: draft.endDate
            val normalizedStart = minOf(nextStart, nextEnd)
            val normalizedEnd = maxOf(nextStart, nextEnd)
            state.copy(
                draft = draft.copy(
                    startDate = normalizedStart,
                    endDate = normalizedEnd,
                    type = DiaryType.between(normalizedStart, normalizedEnd),
                )
            )
        }
    }

    private fun saveDraft() {
        val draft = _state.value.draft
        val previousId = _state.value.composeOriginalId
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            runCatching { repository.saveDraft(draft, previousId) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            screen = DiaryScreenState.Home,
                            composeOriginalId = null,
                        )
                    }
                    AppSnackbarBus.showMessage("Draft saved")
                }
                .onFailure { showFailure("Could not save draft", ::saveDraft) }
        }
    }

    private fun saveDiary() {
        val draft = _state.value.draft
        val previousId = _state.value.composeOriginalId
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            runCatching { repository.saveDiary(draft, previousId) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            screen = DiaryScreenState.Details(draft.id),
                            composeOriginalId = null,
                        )
                    }
                    updateWeeklyRecord(0)
                }
                .onFailure { showFailure("Could not save diary", ::saveDiary) }
        }
    }

    private fun deleteDiary(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteDiary(id) }
                .onSuccess {
                    _state.update { it.copy(screen = DiaryScreenState.Home) }
                    updateWeeklyRecord(0)
                }
                .onFailure { AppSnackbarBus.showMessage("Could not delete diary") }
        }
    }

    private fun syncToTime(id: String) {
        val diary = _state.value.diaries.firstOrNull { it.id == id } ?: return
        if (id in _state.value.syncingIds) return
        viewModelScope.launch {
            _state.update { it.copy(syncingIds = it.syncingIds + id) }
            runCatching { repository.syncDiaryToTime(diary) }
                .onSuccess { AppSnackbarBus.showMessage("Diary event saved to Time") }
                .onFailure { AppSnackbarBus.showApiFailure(onRetry = { syncToTime(id) }) }
            _state.update { it.copy(syncingIds = it.syncingIds - id) }
        }
    }

    private fun removeTimeEvent(id: String) {
        val diary = _state.value.diaries.firstOrNull { it.id == id } ?: return
        if (id in _state.value.syncingIds) return
        viewModelScope.launch {
            _state.update { it.copy(syncingIds = it.syncingIds + id) }
            runCatching { repository.removeTimeEvent(diary) }
                .onSuccess { AppSnackbarBus.showMessage("Diary event removed from Time") }
                .onFailure { AppSnackbarBus.showApiFailure(onRetry = { removeTimeEvent(id) }) }
            _state.update { it.copy(syncingIds = it.syncingIds - id) }
        }
    }

    private fun updateWeeklyRecord(offset: Int) {
        weeklyJob?.cancel()
        weeklyJob = viewModelScope.launch {
            runCatching { repository.getWeeklyRecord(offset) }
                .onSuccess { data -> _state.update { it.copy(weeklyRecordData = data) } }
        }
    }

    private fun showFailure(message: String, retry: () -> Unit) {
        _state.update { it.copy(isSaving = false) }
        AppSnackbarBus.showMessage(message, actionLabel = "Retry", onAction = retry)
    }
}
