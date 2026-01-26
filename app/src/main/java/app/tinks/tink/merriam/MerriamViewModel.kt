package app.tinks.tink.merriam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.merriam.data.Unit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MerriamEvent {
}

data class MerriamUiState(
    val isLoading: Boolean,
)

data class MerriamState(
    val allUnits: List<Unit> = emptyList(),
    val isLoading: Boolean = true,
    val isMerriamChanged: Boolean = false,
    val selectedIndex: Int = 0,
) {
    fun toUiState(): MerriamUiState = MerriamUiState(
        isLoading = isLoading,
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
//        observeLocalMerriams()
    }

    /**
     * 监听本地 Room 数据变化，实时更新 UI。
     * 这样即使在离线时也能立刻看到最新数据。
     */
//    private fun observeLocalMerriams() {
//        viewModelScope.launch {
//            repository.getAllMerriamsFlow().map { it.map { e -> e.toMerriam() } }
//                .collectLatest { Merriams ->
//                    _state.update {
//                        it.copy(
//                            lastMerriam = Merriams.getOrNull(1),
//                            newMerriam = Merriams.getOrNull(1)?.Merriam,
//                            allMerriams = Merriams,
//                            isLoading = false
//                        )
//                    }
//                }
//        }
//    }

    fun onEvent(event: MerriamEvent) {
        when (event) {

            else -> {}
        }
    }
}
