package app.tinks.tink.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.weight.data.Weight
import app.tinks.tink.weight.data.toWeight
import app.tinks.tink.weight.db.WeightDao
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed interface WeightEvent {
    object AddWeight : WeightEvent
    data class DeleteWeight(val id: Int) : WeightEvent
    data class UpdateWeight(val id: Int, val weight: Double) : WeightEvent
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
    val isLoading: Boolean = true,
    val isWeightChanged: Boolean = false,
) {
    fun toUiState(): WeightUiState = WeightUiState(
        isLoading = isLoading,
        weightControlCardUiState = WeightControlCardUiState(
            isTodayRecorded = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(lastWeight?.createdTime ?: 0), ZoneId.systemDefault()
            ) == ZonedDateTime.now(),
            lastDateText = lastWeight?.createdTime?.let {
                Instant.ofEpochMilli(it).atZone(
                    ZoneId.systemDefault()
                ).toLocalDate().format(
                    DateTimeFormatter.ISO_LOCAL_DATE
                )
            } ?: "",
            showConfirm = isWeightChanged,
            newWeight = newWeight,
        ),
        trendChartCardUiState = TrendChartCardUiState(selectedIndex = 0, weightList = emptyList())
    )
}

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repository: WeightRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WeightState())
    val uiState = _state.map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value.toUiState())

    init {
        observeLocalWeights()
    }

    /**
     * 监听本地 Room 数据变化，实时更新 UI。
     * 这样即使在离线时也能立刻看到最新数据。
     */
    private fun observeLocalWeights() {
        viewModelScope.launch {
            repository.getAllWeightsFlow().map { it.map { e -> e.toWeight() } }
                .collectLatest { weights ->
                    _state.update {
                        it.copy(
                            lastWeight = weights.getOrNull(1),
                            newWeight = weights.getOrNull(1)?.weight,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onEvent(event: WeightEvent) {
        when (event) {
            is WeightEvent.AddWeight -> addWeight()
            is WeightEvent.DeleteWeight -> deleteWeight(event.id)
            is WeightEvent.UpdateWeight -> updateWeight(event.id, event.weight)
            WeightEvent.RefreshWeightList -> refreshWeights()
            is WeightEvent.AdjustNewWeight -> adjustNewWeight(event.delta)
            is WeightEvent.ChangeSelectedTrendIndex -> updateChartData(event.index)
        }
    }

    /**
     * 添加体重：立即写入 Room（即使无网），标记为 isSynced = false。
     */
    private fun addWeight() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isWeightChanged = false) }
            _state.value.newWeight?.let { repository.addWeight(it) }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun deleteWeight(id: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.deleteWeight(id)
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun updateWeight(id: Int, weight: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.updateWeight(id, weight)
            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 手动刷新按钮触发（从 Supabase 拉取远程最新数据）
     */
    private fun refreshWeights() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.refreshFromRemote()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun adjustNewWeight(delta: Float) {
        viewModelScope.launch {
            _state.update { it.copy(newWeight = it.newWeight?.plus(delta), isWeightChanged = true) }
        }
    }

    private fun updateChartData(index: Int) {

    }
}


