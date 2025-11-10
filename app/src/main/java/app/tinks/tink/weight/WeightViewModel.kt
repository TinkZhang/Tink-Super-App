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
import javax.inject.Inject

sealed interface WeightEvent {
    data class AddWeight(val weight: Double) : WeightEvent
    data class DeleteWeight(val id: Int) : WeightEvent
    data class UpdateWeight(val id: Int, val weight: Double) : WeightEvent
    object RefreshWeightList : WeightEvent
}

data class WeightUiState(
    val weightOfToday: Weight?,
    val lastWeight: Weight?,
    val isLoading: Boolean,
    val weights: List<Weight>,
)

data class WeightState(
    val weightOfToday: Weight? = null,
    val lastWeight: Weight? = null,
    val isLoading: Boolean = true,
    val weights: List<Weight> = emptyList(),
) {
    fun toUiState(): WeightUiState = WeightUiState(
        weightOfToday = weightOfToday,
        lastWeight = lastWeight,
        isLoading = isLoading,
        weights = weights
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
                            weights = weights,
                            weightOfToday = weights.firstOrNull(),
                            lastWeight = weights.getOrNull(1),
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onEvent(event: WeightEvent) {
        when (event) {
            is WeightEvent.AddWeight -> addWeight(event.weight)
            is WeightEvent.DeleteWeight -> deleteWeight(event.id)
            is WeightEvent.UpdateWeight -> updateWeight(event.id, event.weight)
            WeightEvent.RefreshWeightList -> refreshWeights()
        }
    }

    /**
     * 添加体重：立即写入 Room（即使无网），标记为 isSynced = false。
     */
    private fun addWeight(value: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.addWeight(value)
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
}


