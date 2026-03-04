package app.tinks.tink.time

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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

sealed interface TimeEvent {
    object Refresh : TimeEvent
    data class UpdateStartDate(val value: LocalDate) : TimeEvent
    data class UpdateEndDate(val value: LocalDate) : TimeEvent
    object OpenAddDialog : TimeEvent
    object DismissDialog : TimeEvent
    data class EditEntry(val entry: TimeEntry) : TimeEvent
    data class DeleteEntry(val id: Long) : TimeEvent
    data class UpdateTitle(val value: String) : TimeEvent
    data class UpdateDescription(val value: String) : TimeEvent
    data class UpdateType(val value: Int) : TimeEvent
    data class UpdateStartTime(val value: LocalDateTime) : TimeEvent
    data class UpdateEndTime(val value: LocalDateTime) : TimeEvent
    data class ApplyDurationMinutes(val value: Long) : TimeEvent
    object SaveEntry : TimeEvent
}

data class TimeUiState(
    val isLoading: Boolean,
    val isSaving: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val statistics: List<TimeStatistic>,
    val entriesByDay: List<TimeDayEntries>,
    val deletingIds: Set<Long>,
    val showEditor: Boolean,
    val editor: TimeEditorState,
)

data class TimeDayEntries(
    val day: LocalDate,
    val entries: List<TimeEntry>,
)

data class TimeEditorState(
    val editingId: Long? = null,
    val title: String = "",
    val description: String = "",
    val type: Int = 1,
    val start: LocalDateTime,
    val end: LocalDateTime,
) {
    fun isValid(): Boolean = title.isNotBlank() && end.isAfter(start)

    companion object {
        fun defaultNow(): TimeEditorState {
            val end = LocalDateTime.now().withSecond(0).withNano(0)
            return TimeEditorState(
                start = end.minusMinutes(30),
                end = end,
            )
        }
    }
}

data class TimeState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now(),
    val statistics: List<TimeStatistic> = emptyList(),
    val entriesByDay: List<TimeDayEntries> = emptyList(),
    val deletingIds: Set<Long> = emptySet(),
    val showEditor: Boolean = false,
    val editor: TimeEditorState = TimeEditorState.defaultNow(),
) {
    fun toUiState(): TimeUiState = TimeUiState(
        isLoading = isLoading,
        isSaving = isSaving,
        startDate = startDate,
        endDate = endDate,
        statistics = statistics,
        entriesByDay = entriesByDay,
        deletingIds = deletingIds,
        showEditor = showEditor,
        editor = editor,
    )
}

@HiltViewModel
class TimeViewModel @Inject constructor(
    private val repository: TimeRepository,
) : ViewModel() {
    private val localZone = ZoneId.systemDefault()
    private val _state = MutableStateFlow(TimeState())

    val uiState = _state.map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value.toUiState())

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun onEvent(event: TimeEvent) {
        when (event) {
            is TimeEvent.Refresh -> refresh()
            is TimeEvent.UpdateStartDate -> updateStartDate(event.value)
            is TimeEvent.UpdateEndDate -> updateEndDate(event.value)
            is TimeEvent.OpenAddDialog -> openAddDialog()
            is TimeEvent.DismissDialog -> closeDialog()
            is TimeEvent.EditEntry -> openEditDialog(event.entry)
            is TimeEvent.DeleteEntry -> deleteEntry(event.id)
            is TimeEvent.UpdateTitle -> _state.update {
                it.copy(editor = it.editor.copy(title = event.value))
            }
            is TimeEvent.UpdateDescription -> _state.update {
                it.copy(editor = it.editor.copy(description = event.value))
            }
            is TimeEvent.UpdateType -> _state.update {
                it.copy(editor = it.editor.copy(type = event.value))
            }
            is TimeEvent.UpdateStartTime -> _state.update {
                it.copy(editor = it.editor.copy(start = event.value))
            }
            is TimeEvent.UpdateEndTime -> _state.update {
                it.copy(editor = it.editor.copy(end = event.value))
            }
            is TimeEvent.ApplyDurationMinutes -> _state.update {
                val end = it.editor.end
                it.copy(editor = it.editor.copy(start = end.minusMinutes(event.value)))
            }
            is TimeEvent.SaveEntry -> saveEntry()
        }
    }

    private fun openAddDialog() {
        _state.update {
            it.copy(
                showEditor = true,
                editor = TimeEditorState.defaultNow(),
            )
        }
    }

    private fun openEditDialog(entry: TimeEntry) {
        _state.update {
            it.copy(
                showEditor = true,
                editor = TimeEditorState(
                    editingId = entry.id,
                    title = entry.title,
                    description = entry.description.orEmpty(),
                    type = entry.type,
                    start = entry.start.atZoneSameInstant(localZone).toLocalDateTime(),
                    end = entry.end.atZoneSameInstant(localZone).toLocalDateTime(),
                )
            )
        }
    }

    private fun closeDialog() {
        _state.update {
            it.copy(
                showEditor = false,
                isSaving = false,
                editor = TimeEditorState.defaultNow(),
            )
        }
    }

    private fun updateStartDate(value: LocalDate) {
        _state.update {
            val newEnd = if (value > it.endDate) value else it.endDate
            it.copy(startDate = value, endDate = newEnd)
        }
        refresh()
    }

    private fun updateEndDate(value: LocalDate) {
        _state.update {
            val newStart = if (value < it.startDate) value else it.startDate
            it.copy(startDate = newStart, endDate = value)
        }
        refresh()
    }

    private fun refresh() {
        refreshJob?.cancel()
        val startDate = _state.value.startDate
        val endDate = _state.value.endDate
        refreshJob = repository.getTimeDashboard(startDate = startDate, endDate = endDate)
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> _state.update {
                        it.copy(isLoading = true)
                    }

                    is ApiResult.Success -> _state.update {
                        it.copy(
                            isLoading = false,
                            statistics = result.data.statistics,
                            entriesByDay = result.data.entries.toDayEntries(localZone),
                        )
                    }

                    is ApiResult.Error -> _state.update {
                        it.copy(isLoading = false)
                    }.also {
                        AppSnackbarBus.showApiFailure(onRetry = ::refresh)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun saveEntry() {
        val currentEditor = _state.value.editor
        if (!currentEditor.isValid()) {
            AppSnackbarBus.showMessage("Please enter a title and keep end time after start time.")
            return
        }

        val payload = TimeUpsertRequest(
            type = currentEditor.type,
            start = currentEditor.start.atZone(localZone).toInstant().toString(),
            end = currentEditor.end.atZone(localZone).toInstant().toString(),
            title = currentEditor.title.trim(),
            description = currentEditor.description.takeIf { it.isNotBlank() },
        )

        val saveFlow = if (currentEditor.editingId == null) {
            repository.createTimeEntry(payload = payload)
        } else {
            repository.updateTimeEntry(timeId = currentEditor.editingId, payload = payload)
        }

        saveFlow.onEach { result ->
            when (result) {
                is ApiResult.Loading -> _state.update {
                    it.copy(isSaving = true)
                }

                is ApiResult.Success -> _state.update {
                    it.copy(
                        isSaving = false,
                        showEditor = false,
                        editor = TimeEditorState.defaultNow(),
                    )
                }.also {
                    refresh()
                }

                is ApiResult.Error -> _state.update {
                    it.copy(isSaving = false)
                }.also {
                    AppSnackbarBus.showApiFailure(onRetry = ::saveEntry)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun deleteEntry(id: Long) {
        if (id in _state.value.deletingIds) return
        val previous = _state.value.entriesByDay
        _state.update {
            it.copy(
                deletingIds = it.deletingIds + id,
                entriesByDay = it.entriesByDay.removeEntry(id),
            )
        }
        repository.deleteTimeEntry(timeId = id).onEach { result ->
            when (result) {
                is ApiResult.Loading -> Unit
                is ApiResult.Success -> _state.update {
                    it.copy(deletingIds = it.deletingIds - id)
                }

                is ApiResult.Error -> _state.update {
                    it.copy(
                        deletingIds = it.deletingIds - id,
                        entriesByDay = previous,
                    )
                }.also {
                    AppSnackbarBus.showApiFailure { deleteEntry(id) }
                }
            }
        }.launchIn(viewModelScope)
    }
}

private fun List<TimeEntry>.toDayEntries(zoneId: ZoneId): List<TimeDayEntries> {
    return groupBy { it.start.atZoneSameInstant(zoneId).toLocalDate() }
        .entries
        .sortedByDescending { it.key }
        .map { (day, entries) ->
            TimeDayEntries(
                day = day,
                entries = entries.sortedBy { it.start.toInstant() },
            )
        }
}

private fun List<TimeDayEntries>.removeEntry(id: Long): List<TimeDayEntries> {
    return mapNotNull { section ->
        val next = section.entries.filterNot { it.id == id }
        if (next.isEmpty()) {
            null
        } else {
            section.copy(entries = next)
        }
    }
}
