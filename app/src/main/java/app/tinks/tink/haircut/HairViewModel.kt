package app.tinks.tink.haircut

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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

sealed interface HaircutEvent {
    object AddHaircutFabClick : HaircutEvent
    object DismissDialog : HaircutEvent
    data class SubmitHaircut(val price: Int, val shopName: String, val date: LocalDate) :
        HaircutEvent

    data class DeleteHaircut(val id: Int) : HaircutEvent
    object RefreshHaircutList : HaircutEvent

    data class ChangeSelectedTrendIndex(val index: Int) : HaircutEvent
}

data class HaircutUiState(
    val isLoading: Boolean,
    val history: List<Haircut>,
    val days: Int,
    val showDialog: Boolean = false,
)

data class HaircutState(
    val history: List<Haircut> = emptyList(),
    val days: Int = 0,
    val isLoading: Boolean = true,
    val showDialog: Boolean = false,
) {
    fun toUiState(): HaircutUiState = HaircutUiState(
        isLoading = isLoading,
        history = history,
        days = days,
        showDialog = showDialog,
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
    @OptIn(ExperimentalTime::class)
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
                                val today = Clock.System.now()
                                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                                val firstDay = haircuts.first().date
                                today.toEpochDays() - firstDay.toEpochDays()
                            }).toInt(),
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onEvent(event: HaircutEvent) {
        when (event) {
            is HaircutEvent.AddHaircutFabClick -> _state.update { it.copy(showDialog = true) }
            is HaircutEvent.DismissDialog -> _state.update { it.copy(showDialog = false) }
            is HaircutEvent.SubmitHaircut -> submitHaircut(event.price, event.shopName, event.date)
            is HaircutEvent.DeleteHaircut -> deleteHaircut(event.id)
            HaircutEvent.RefreshHaircutList -> refreshHaircuts()
            is HaircutEvent.ChangeSelectedTrendIndex -> updateChartData(event.index)
        }
    }

    /**
     * 提交理发记录：立即写入 Room（即使无网），标记为 isSynced = false。
     */
    private fun submitHaircut(price: Int, shopName: String, date: LocalDate) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, showDialog = false) }
            repository.addHaircut(
                price = price,
                shopName = shopName,
                date = date
            )
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
            try {
                _state.update { it.copy(isLoading = true) }
                repository.refreshFromRemote()
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                e.printStackTrace()
            }
        }
    }

    private fun updateChartData(index: Int) {

    }
}

