package app.tinks.tink.zi.zilist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.zi.Zi
import app.tinks.tink.zi.ZiRepository
import app.tinks.tink.zi.toZi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ZiListEvent {
    data class DeleteZi(val id: Int) : ZiListEvent
    data class UpdateZi(val id: Int, val Zi: String) : ZiListEvent
    object Refresh : ZiListEvent
    data class AdjustNewZi(val delta: Float) : ZiListEvent
}

data class ZiListUiState(
    val isLoading: Boolean,
    val zis: List<Zi> = emptyList(),
)

data class ZiListState(
    val lastZi: Zi? = null,
    val newZi: Double? = null,
    val allZis: List<Zi> = emptyList(),
    val isLoading: Boolean = true,
    val isZiChanged: Boolean = false,
    val selectedIndex: Int = 0,
) {
    fun toUiState() = ZiListUiState(
        isLoading = isLoading,
        zis = allZis
    )
}

@HiltViewModel
class LearntZiListViewModel @Inject constructor(
    private val repository: ZiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ZiListState())
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
            repository.getAllLearntZisFlow().map { it.map { e -> e.toZi() } }
                .collectLatest { zis ->
                    _state.update {
                        it.copy(
                            allZis = zis,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onEvent(event: ZiListEvent) {
        when (event) {
            is ZiListEvent.DeleteZi -> deleteZi(event.id)
            is ZiListEvent.UpdateZi -> updateZi(event.id, event.Zi)
            ZiListEvent.Refresh -> refreshZis()
            is ZiListEvent.AdjustNewZi -> adjustNewZi(event.delta)
        }
    }


    private fun deleteZi(id: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.deleteZi(id)
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun updateZi(id: Int, Zi: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.updateZi(id, Zi)
            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 手动刷新按钮触发（从 Supabase 拉取远程最新数据）
     */
    private fun refreshZis() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.refreshFromRemote()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun adjustNewZi(delta: Float) {
        viewModelScope.launch {
            _state.update { it.copy(newZi = it.newZi?.plus(delta), isZiChanged = true) }
        }
    }
}