package app.tinks.tink.merriam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.merriam.data.Unit
import app.tinks.tink.merriam.network.RootPostDto
import app.tinks.tink.network.ApiResult
import app.tinks.tink.ui.components.AppSnackbarBus
import app.tinks.tink.ui.components.WeeklyRecordData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MerriamEvent {
    data class CompleteRoot(val id: Int) : MerriamEvent
    object Refresh : MerriamEvent
}

data class MerriamUiState(
    val isLoading: Boolean,
    val units: List<Unit>,
    val latest: Int,
    val weeklyRecords: WeeklyRecordData,
)

data class MerriamState(
    val allUnits: List<Unit> = emptyList(),
    val isLoading: Boolean = true,
    val isNetworkError: Boolean = false,
    val latest: Int = 0,
    val weeklyRecordData: WeeklyRecordData = WeeklyRecordData(null, emptyList())
) {
    fun toUiState(): MerriamUiState = MerriamUiState(
        isLoading = isLoading,
        units = allUnits,
        latest = latest,
        weeklyRecords = weeklyRecordData,
    )
}

@HiltViewModel
class MerriamViewModel @Inject constructor(
    private val repository: MerriamRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MerriamState())
    val uiState = _state.map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value.toUiState())

    init {
        loadStat()
        observeLocalMerriams()
    }

    private fun observeLocalMerriams() {
        viewModelScope.launch {
            repository.getAllUnitsFlow().collectLatest { units ->
                _state.update {
                    it.copy(
                        allUnits = units
                    )
                }
            }
        }
    }

    private fun loadStat() {
        repository.getMerriamStat().onEach { result ->
            when (result) {
                is ApiResult.Loading -> _state.update {
                    it.copy(
                        isLoading = true, isNetworkError = false
                    )
                }

                is ApiResult.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        isNetworkError = false,
                        latest = result.data.latest,
                        weeklyRecordData = WeeklyRecordData.fromWeeklyInt(result.data.weekStats),
                    )
                }

                is ApiResult.Error -> _state.update {
                    it.copy(
                        isLoading = false, isNetworkError = true
                    )
                }.also {
                    AppSnackbarBus.showApiFailure(onRetry = ::loadStat)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun addRecords(records: List<RootPostDto>) {
        repository.addMerriamRecords(records).onEach { result ->
            when (result) {
                is ApiResult.Loading -> _state.update {
                    it.copy(
                        isLoading = true, isNetworkError = false
                    )
                }

                is ApiResult.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        isNetworkError = false,
                    )
                }

                is ApiResult.Error -> _state.update {
                    it.copy(
                        isLoading = false, isNetworkError = true
                    )
                }.also {
                    AppSnackbarBus.showApiFailure {
                        addRecords(records)
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun buildRecordsToComplete(clickedId: Int): List<RootPostDto> {
        val latest = _state.value.latest
        if (clickedId <= latest) return emptyList()

        return _state.value.allUnits
            .flatMap { it.roots }
            .sortedBy { it.id }
            .asSequence()
            .filter { it.id in (latest + 1)..clickedId }
            .map { RootPostDto(rootId = it.id, root = it.text) }
            .toList()
    }

    fun onEvent(event: MerriamEvent) {
        when (event) {
            is MerriamEvent.CompleteRoot -> {
                val records = buildRecordsToComplete(event.id)
                if (records.isNotEmpty()) {
                    addRecords(records)
                    _state.update { it.copy(latest = records.last().rootId) }
                }
            }

            is MerriamEvent.Refresh -> loadStat()
        }
    }
}
