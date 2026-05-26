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
    data class ApplyLabel(val label: TimeLabel) : TimeEvent
    object SaveEntry : TimeEvent
    object OpenLabelManager : TimeEvent
    object DismissLabelManager : TimeEvent
    data class UpdateLabelManagerType(val value: Int) : TimeEvent
    data class UpdateLabelDraft(val value: String) : TimeEvent
    data class EditLabel(val label: TimeLabel) : TimeEvent
    object SaveLabel : TimeEvent
    data class DeleteLabel(val id: Long) : TimeEvent
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
    val labels: List<TimeLabel>,
    val showLabelManager: Boolean,
    val labelManager: TimeLabelManagerState,
)

data class TimeDayEntries(
    val day: LocalDate,
    val entries: List<TimeEntry>,
)

data class TimeEditorState(
    val editingId: Long? = null,
    val title: String = "",
    val description: String = "",
    val type: Int = DEFAULT_TIME_TYPE,
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

data class TimeLabelManagerState(
    val selectedType: Int = DEFAULT_TIME_TYPE,
    val editingId: Long? = null,
    val draftName: String = "",
    val isSaving: Boolean = false,
    val deletingIds: Set<Long> = emptySet(),
) {
    fun isValid(): Boolean = draftName.isNotBlank()
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
    val labels: List<TimeLabel> = emptyList(),
    val showLabelManager: Boolean = false,
    val labelManager: TimeLabelManagerState = TimeLabelManagerState(),
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
        labels = labels,
        showLabelManager = showLabelManager,
        labelManager = labelManager,
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
    private var labelsJob: Job? = null

    init {
        refresh()
        refreshLabels()
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
            is TimeEvent.ApplyLabel -> applyLabel(event.label)
            is TimeEvent.SaveEntry -> saveEntry()
            is TimeEvent.OpenLabelManager -> openLabelManager()
            is TimeEvent.DismissLabelManager -> closeLabelManager()
            is TimeEvent.UpdateLabelManagerType -> _state.update {
                it.copy(labelManager = TimeLabelManagerState(selectedType = event.value))
            }
            is TimeEvent.UpdateLabelDraft -> _state.update {
                it.copy(labelManager = it.labelManager.copy(draftName = event.value))
            }
            is TimeEvent.EditLabel -> _state.update {
                it.copy(
                    labelManager = it.labelManager.copy(
                        selectedType = event.label.type,
                        editingId = event.label.id,
                        draftName = event.label.name,
                    )
                )
            }
            is TimeEvent.SaveLabel -> saveLabel()
            is TimeEvent.DeleteLabel -> deleteLabel(event.id)
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
                        val filteredEntries = result.data.entries.filterInDateRange(
                            startDate = startDate,
                            endDate = endDate,
                            zoneId = localZone,
                        )
                        it.copy(
                            isLoading = false,
                            statistics = result.data.statistics,
                            entriesByDay = filteredEntries.toDayEntries(localZone),
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

    private fun refreshLabels() {
        labelsJob?.cancel()
        labelsJob = repository.getTimeLabels()
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> Unit
                    is ApiResult.Success -> _state.update {
                        it.copy(labels = result.data.sortedForDisplay())
                    }
                    is ApiResult.Error -> AppSnackbarBus.showApiFailure(onRetry = ::refreshLabels)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun applyLabel(label: TimeLabel) {
        _state.update { state ->
            val editor = state.editor
            val knownPrefixes = state.labels
                .filter { it.type == editor.type }
                .map { "${it.name}:" }
            val trimmed = editor.title.trim()
            val titleWithoutExistingLabel = knownPrefixes.firstNotNullOfOrNull { prefix ->
                trimmed.takeIf { it.startsWith(prefix) }
                    ?.removePrefix(prefix)
                    ?.trimStart()
            } ?: trimmed
            val nextTitle = if (titleWithoutExistingLabel.isBlank()) {
                "${label.name}: "
            } else {
                "${label.name}: $titleWithoutExistingLabel"
            }
            state.copy(editor = editor.copy(title = nextTitle))
        }
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

    private fun openLabelManager() {
        _state.update {
            it.copy(
                showLabelManager = true,
                labelManager = TimeLabelManagerState(
                    selectedType = it.editor.type.takeIf { type -> type in 1..11 } ?: DEFAULT_TIME_TYPE,
                ),
            )
        }
    }

    private fun closeLabelManager() {
        _state.update {
            it.copy(
                showLabelManager = false,
                labelManager = TimeLabelManagerState(),
            )
        }
    }

    private fun saveLabel() {
        val manager = _state.value.labelManager
        if (!manager.isValid()) {
            AppSnackbarBus.showMessage("Please enter a label.")
            return
        }

        val saveFlow = if (manager.editingId == null) {
            repository.createTimeLabel(
                type = manager.selectedType,
                name = manager.draftName,
            )
        } else {
            repository.updateTimeLabel(
                labelId = manager.editingId,
                type = manager.selectedType,
                name = manager.draftName,
            )
        }

        saveFlow.onEach { result ->
            when (result) {
                is ApiResult.Loading -> _state.update {
                    it.copy(labelManager = it.labelManager.copy(isSaving = true))
                }
                is ApiResult.Success -> _state.update {
                    val nextLabels = (it.labels.filterNot { label -> label.id == result.data.id } + result.data)
                        .sortedForDisplay()
                    it.copy(
                        labels = nextLabels,
                        labelManager = TimeLabelManagerState(selectedType = result.data.type),
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(labelManager = it.labelManager.copy(isSaving = false))
                }.also {
                    AppSnackbarBus.showApiFailure(onRetry = ::saveLabel)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun deleteLabel(id: Long) {
        val previousLabels = _state.value.labels
        _state.update {
            it.copy(
                labels = it.labels.filterNot { label -> label.id == id },
                labelManager = it.labelManager.copy(deletingIds = it.labelManager.deletingIds + id),
            )
        }
        repository.deleteTimeLabel(labelId = id).onEach { result ->
            when (result) {
                is ApiResult.Loading -> Unit
                is ApiResult.Success -> _state.update {
                    it.copy(
                        labelManager = it.labelManager.copy(deletingIds = it.labelManager.deletingIds - id),
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(
                        labels = previousLabels,
                        labelManager = it.labelManager.copy(deletingIds = it.labelManager.deletingIds - id),
                    )
                }.also {
                    AppSnackbarBus.showApiFailure { deleteLabel(id) }
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

internal fun List<TimeEntry>.filterInDateRange(
    startDate: LocalDate,
    endDate: LocalDate,
    zoneId: ZoneId,
): List<TimeEntry> {
    return filter { entry ->
        val day = entry.start.atZoneSameInstant(zoneId).toLocalDate()
        day >= startDate && day <= endDate
    }
}

private fun List<TimeLabel>.sortedForDisplay(): List<TimeLabel> =
    sortedWith(compareBy<TimeLabel> { it.type }.thenBy { it.sortOrder }.thenBy { it.name })

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

const val DEFAULT_TIME_TYPE = 5
