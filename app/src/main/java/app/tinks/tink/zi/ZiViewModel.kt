package app.tinks.tink.zi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.ui.components.AppSnackbarBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

sealed interface ZiEvent {
    data class AddZi(val zis: String, val proficiency: Int, val date: LocalDate) : ZiEvent
    data class DeleteZi(val id: Int) : ZiEvent
    data class UpdateZi(val id: Int, val Zi: String) : ZiEvent
    object Refresh : ZiEvent
    object DismissDialog : ZiEvent
    object AddZiDialogOpen : ZiEvent
}

data class ZiUiState(
    val isLoading: Boolean,
    val showDialog: Boolean,
    val learntZiNum: Int = 0,
    val reviewList: List<Zi> = emptyList(),
)

data class TrendChartCardUiState(
    val selectedIndex: Int,
    val ZiList: List<Zi>,
)


data class ZiState(
    val learntZiNum: Int = 0,
    val showDialog: Boolean = false,
    val allZis: List<Zi> = emptyList(),
    val isLoading: Boolean = true,
    val isZiChanged: Boolean = false,
    val selectedIndex: Int = 0,
    val reviewList: List<Zi> = emptyList(),
) {
    fun toUiState(): ZiUiState = ZiUiState(
        isLoading = isLoading,
        showDialog = showDialog,
        learntZiNum = learntZiNum,
        reviewList = reviewList,
    )
}

@HiltViewModel
class ZiViewModel @Inject constructor(
    private val repository: ZiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ZiState())
    val uiState = _state.map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value.toUiState())

    init {
        observeLocalZis()
    }

    /**
     * 监听本地 Room 数据变化，实时更新 UI。
     * 这样即使在离线时也能立刻看到最新数据。
     */
    private fun observeLocalZis() {
        viewModelScope.launch {
            repository.getAllZisFlow().map { it.map { e -> e.toZi() } }
                .collectLatest { Zis ->
                    _state.update { state ->
                        state.copy(
                            learntZiNum = Zis.count { it.proficiency > 4 },
                            allZis = Zis,
                            isLoading = false,
                            reviewList = Zis
                                .asSequence() // optional but more efficient for larger lists
                                .filter { it.proficiency < 5 }
                                .sortedWith(
                                    compareBy<Zi> { it.proficiency }
                                        .thenBy { it.lastDate }
                                )
                                .take(9)
                                .toList()
                        )
                    }
                }
        }
    }

    fun onEvent(event: ZiEvent) {
        when (event) {
            is ZiEvent.AddZi -> addZi(event.proficiency, event.zis, event.date)
            is ZiEvent.DeleteZi -> deleteZi(event.id)
            is ZiEvent.UpdateZi -> updateZi(event.id, event.Zi)
            ZiEvent.Refresh -> refreshZis()
            is ZiEvent.DismissDialog -> _state.update { it.copy(showDialog = false) }
            is ZiEvent.AddZiDialogOpen -> _state.update { it.copy(showDialog = true) }
        }
    }

    /**
     * 添加体重：立即写入 Room（即使无网），标记为 isSynced = false。
     */
    private fun addZi(proficiency: Int, zis: String, date: LocalDate) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isZiChanged = false, showDialog = false) }
            zis.forEach { zi ->  repository.addZi(zi = zi.toString(), date = date, proficiency = proficiency) }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun deleteZi(id: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.deleteZi(id)
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun updateZi(id: Int, zi: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.updateZi(id, zi)
            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 手动刷新按钮触发（从 Supabase 拉取远程最新数据）
     */
    private fun refreshZis() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                repository.refreshFromRemote()
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                AppSnackbarBus.showApiFailure(onRetry = ::refreshZis)
            }
        }
    }
}
