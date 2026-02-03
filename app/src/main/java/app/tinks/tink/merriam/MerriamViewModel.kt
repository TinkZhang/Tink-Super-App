package app.tinks.tink.merriam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.merriam.data.Unit
import app.tinks.tink.merriam.db.toRoot
import app.tinks.tink.ui.components.WeeklyRecordData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MerriamEvent {
    data class CompleteRoot(val id: Int) : MerriamEvent
}

data class MerriamUiState(
    val isLoading: Boolean,
    val units: List<Unit>,
    val weeklyRecords: WeeklyRecordData,
)

data class MerriamState(
    val allUnits: List<Unit> = emptyList(),
    val isLoading: Boolean = true,
    val isMerriamChanged: Boolean = false,
    val selectedIndex: Int = 0,
    val weeklyRecordData: WeeklyRecordData = WeeklyRecordData(null, emptyList())
) {
    fun toUiState(): MerriamUiState = MerriamUiState(
        isLoading = isLoading,
        units = allUnits,
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
        observeLocalMerriams()
    }

    /**
     * 监听本地 Room 数据变化，实时更新 UI。
     * 这样即使在离线时也能立刻看到最新数据。
     */
    private fun observeLocalMerriams() {
        viewModelScope.launch {
            repository.getAllMerriamsFlow().map { roots ->
                roots.groupBy { it.unit }
                    .map { (unitId, group) -> Unit(id = unitId, roots = group.map { it.toRoot() }) }
            }
                .collectLatest { units ->
                    _state.update {
                        it.copy(
                            allUnits = units
                        )
                    }
                }
        }
    }

    fun onEvent(event: MerriamEvent) {
        when (event) {
            is MerriamEvent.CompleteRoot -> {
                repository.addMerriamRecord(event.id)
            }
            else -> {}
        }
    }
}
