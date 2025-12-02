package app.tinks.tink.haircut

import android.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.haircut.data.Haircut
import app.tinks.tink.haircut.data.toHaircut
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.minus
import java.time.LocalDate
import javax.inject.Inject

sealed interface HaircutEvent {
    object AddHaircutFabClick : HaircutEvent
    data class DeleteHaircut(val id: Int) : HaircutEvent
    object RefreshHaircutList : HaircutEvent

    data class ChangeSelectedTrendIndex(val index: Int) : HaircutEvent
}

data class HaircutUiState(
    val isLoading: Boolean,
    val history: List<Haircut>,
    val days: Int,
)

data class HaircutState(
    val history: List<Haircut> = emptyList(),
    val days: Int = 0,
    val isLoading: Boolean = true,
    val isHaircutChanged: Boolean = false,
    val newHaircutPrice: Int? = null,
    val newHaircutShopName: String? = null,
) {
    fun toUiState(): HaircutUiState = HaircutUiState(
        isLoading = isLoading,
        history = history,
        days = days,
    )
}

@HiltViewModel
class HaircutViewModel @Inject constructor(
    private val repository: HaircutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HaircutState())
    val uiState = _state.map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value.toUiState())

    init {
        observeLocalHaircuts()
    }

    /**
     * 监听本地 Room 数据变化，实时更新 UI。
     * 这样即使在离线时也能立刻看到最新数据。
     */
    private fun observeLocalHaircuts() {
        viewModelScope.launch {
            repository.getAllHaircutsFlow().map { it.map { e -> e.toHaircut() } }
                .collectLatest { haircuts ->
                    _state.update {
                        it.copy(
                            history = haircuts,
                            days = (if (haircuts.isEmpty()) {
                                -1
                            } else {
                                val today = LocalDate.now()
                                val firstDay = haircuts.first().date
                                today.toEpochDay() - firstDay.toEpochDays()
                            }).toInt(),
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onEvent(event: HaircutEvent) {
        when (event) {
            is HaircutEvent.AddHaircutFabClick -> addHaircut()
            is HaircutEvent.DeleteHaircut -> deleteHaircut(event.id)
            HaircutEvent.RefreshHaircutList -> refreshHaircuts()
            is HaircutEvent.ChangeSelectedTrendIndex -> updateChartData(event.index)
        }
    }

    /**
     * 添加体重：立即写入 Room（即使无网），标记为 isSynced = false。
     */
    private fun addHaircut() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isHaircutChanged = false) }
            _state.value.newHaircutPrice?.let {
                repository.addHaircut(
                    price = it,
                    shopName = _state.value.newHaircutShopName ?: ""
                )
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun deleteHaircut(id: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.deleteHaircut(id)
            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 手动刷新按钮触发（从 Supabase 拉取远程最新数据）
     */
    private fun refreshHaircuts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.refreshFromRemote()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun updateChartData(index: Int) {

    }
}


