package app.tinks.tink.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.network.ApiResult
import app.tinks.tink.ui.components.AppSnackbarBus
import app.tinks.tink.weight.data.Weight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed interface WeightEvent {
    object AddWeight : WeightEvent
    data class DeleteWeight(val id: Int) : WeightEvent
    object RefreshWeightList : WeightEvent
    data class AdjustNewWeight(val delta: Float) : WeightEvent
    data class ChangeSelectedTrendIndex(val index: Int) : WeightEvent
}

data class WeightUiState(
    val isLoading: Boolean,
    val weightControlCardUiState: WeightControlCardUiState,
    val trendChartCardUiState: TrendChartCardUiState,
)

data class TrendChartCardUiState(
    val selectedIndex: Int,
    val weightList: List<Weight>,
)

data class WeightControlCardUiState(
    val isTodayRecorded: Boolean,
    val lastDateText: String,
    val showConfirm: Boolean,
    val newWeight: Double?,
)

data class WeightState(
    val lastWeight: Weight? = null,
    val newWeight: Double? = null,
    val allWeights: List<Weight> = emptyList(),
    val isLoading: Boolean = false,
    val isWeightChanged: Boolean = false,
    val selectedIndex: Int = 0,
) {
    fun toUiState(): WeightUiState {
        val latestDate = lastWeight?.createdTime?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        return WeightUiState(
            isLoading = isLoading,
            weightControlCardUiState = WeightControlCardUiState(
                isTodayRecorded = latestDate == LocalDate.now(),
                lastDateText = latestDate?.format(DateTimeFormatter.ISO_LOCAL_DATE).orEmpty(),
                showConfirm = isWeightChanged,
                newWeight = newWeight,
            ),
            trendChartCardUiState = TrendChartCardUiState(
                selectedIndex = selectedIndex,
                weightList = getTrendWeights(allWeights, selectedIndex),
            )
        )
    }

    private fun getTrendWeights(weights: List<Weight>, selectedIndex: Int): List<Weight> {
        val sortedWeights = weights.sortedBy { it.createdTime }
        if (selectedIndex != 0) {
            return sortedWeights
        }

        val currentMonth = YearMonth.now()
        return sortedWeights.filter { weight ->
            val weightDate = Instant.ofEpochMilli(weight.createdTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            YearMonth.from(weightDate) == currentMonth
        }
    }
}

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repository: WeightRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WeightState())
    val uiState = _state.map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value.toUiState())

    private var refreshJob: Job? = null

    init {
        refreshWeights()
    }

    fun onEvent(event: WeightEvent) {
        when (event) {
            is WeightEvent.AddWeight -> addWeight()
            is WeightEvent.DeleteWeight -> deleteWeight(event.id)
            WeightEvent.RefreshWeightList -> refreshWeights()
            is WeightEvent.AdjustNewWeight -> adjustNewWeight(event.delta)
            is WeightEvent.ChangeSelectedTrendIndex -> updateChartData(event.index)
        }
    }

    private fun addWeight() {
        val weight = _state.value.newWeight ?: return
        repository.addWeight(weight)
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> _state.update {
                        it.copy(isLoading = true, isWeightChanged = false)
                    }

                    is ApiResult.Success -> refreshWeights(resetDraft = true)

                    is ApiResult.Error -> _state.update {
                        it.copy(isLoading = false, isWeightChanged = true)
                    }.also {
                        AppSnackbarBus.showApiFailure(onRetry = ::addWeight)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun deleteWeight(id: Int) {
        repository.deleteWeight(id)
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> _state.update {
                        it.copy(isLoading = true)
                    }

                    is ApiResult.Success -> refreshWeights(resetDraft = true)

                    is ApiResult.Error -> _state.update {
                        it.copy(isLoading = false)
                    }.also {
                        AppSnackbarBus.showApiFailure(onRetry = { deleteWeight(id) })
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun refreshWeights(resetDraft: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = repository.getWeights()
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> _state.update {
                        it.copy(isLoading = true)
                    }

                    is ApiResult.Success -> _state.update { state ->
                        val latestWeight = result.data.firstOrNull()
                        state.copy(
                            lastWeight = latestWeight,
                            newWeight = when {
                                resetDraft || !state.isWeightChanged -> latestWeight?.weight
                                    ?: state.newWeight

                                else -> state.newWeight
                            },
                            allWeights = result.data,
                            isLoading = false,
                            isWeightChanged = if (resetDraft) false else state.isWeightChanged,
                        )
                    }

                    is ApiResult.Error -> _state.update {
                        it.copy(isLoading = false)
                    }.also {
                        AppSnackbarBus.showApiFailure(onRetry = { refreshWeights(resetDraft) })
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun adjustNewWeight(delta: Float) {
        _state.update {
            it.copy(
                newWeight = it.newWeight?.plus(delta),
                isWeightChanged = true,
            )
        }
    }

    private fun updateChartData(index: Int) {
        _state.update { it.copy(selectedIndex = index) }
    }
}
