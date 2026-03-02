package app.tinks.tink.zi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.network.ApiResult
import app.tinks.tink.ui.components.AppSnackbarBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ZiEvent {
    data class UpdateZi(val proficiency: Int, val zis: String) : ZiEvent
    object Refresh : ZiEvent
    object DismissDialog : ZiEvent
    object AddZiDialogOpen : ZiEvent
    object GenerateStory : ZiEvent
}

data class ZiUiState(
    val isLoading: Boolean,
    val showDialog: Boolean,
    val learntZiNum: Int = 0,
    val allZis: List<Zi> = emptyList(),
    val isNetworkError: Boolean = false,
    val reviewList: List<Zi> = emptyList(),
)

data class ZiState(
    val learntZiNum: Int = 0,
    val showDialog: Boolean = false,
    val isNetworkError: Boolean = false,
    val allZis: List<Zi> = emptyList(),
    val isLoading: Boolean = true,
    val selectedIndex: Int = 0,
    val reviewList: List<Zi> = emptyList(),
) {
    fun toUiState(): ZiUiState = ZiUiState(
        isLoading = isLoading,
        showDialog = showDialog,
        learntZiNum = learntZiNum,
        reviewList = reviewList,
        isNetworkError = isNetworkError,
        allZis = allZis,
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
        refreshAllData()
    }

    fun onEvent(event: ZiEvent) {
        when (event) {
            is ZiEvent.UpdateZi -> {
                _state.update { it.copy(showDialog = false) }
                updateZi(
                    event.proficiency,
                    event.zis.filterNot { it.isWhitespace() })
            }

            ZiEvent.Refresh -> refreshAllData()
            is ZiEvent.DismissDialog -> _state.update { it.copy(showDialog = false) }
            is ZiEvent.AddZiDialogOpen -> _state.update { it.copy(showDialog = true) }
            ZiEvent.GenerateStory -> generateStory()
        }
    }

    fun refreshAllData() {
        repository.getAllZis().onEach { result ->
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
                        allZis = result.data,
                        learntZiNum = result.data.filter { it.proficiency == 5 }.size,
                        reviewList = result.data.filter { it.proficiency < 5 }
                            .sortedBy { it.proficiency }.take(9),
                    )
                }

                is ApiResult.Error -> _state.update {
                    it.copy(
                        isLoading = false, isNetworkError = true
                    )
                }.also {
                    AppSnackbarBus.showApiFailure(onRetry = ::refreshAllData)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun updateZi(proficiency: Int, zis: String) {
        repository.putZi(zis, proficiency).onEach { result ->
            when (result) {
                is ApiResult.Loading -> _state.update {
                    it.copy(
                        isLoading = true, isNetworkError = false
                    )
                }

                is ApiResult.Success -> _state.update {
                    refreshAllData()
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
                    AppSnackbarBus.showApiFailure(onRetry = ::refreshAllData)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun generateStory() {
        viewModelScope.launch {
            val length = repository.getStoryLength()
            repository.generateStory(length = length).onEach { result ->
                when (result) {
                    is ApiResult.Loading -> _state.update {
                        it.copy(isLoading = true)
                    }

                    is ApiResult.Success -> _state.update {
                        it.copy(isLoading = false)
                    }.also {
                        AppSnackbarBus.showMessage("故事生成成功")
                    }

                    is ApiResult.Error -> _state.update {
                        it.copy(isLoading = false)
                    }.also {
                        AppSnackbarBus.showMessage("故事生成失败")
                    }
                }
            }.launchIn(viewModelScope)
        }
    }
}
