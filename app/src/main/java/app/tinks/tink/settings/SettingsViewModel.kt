package app.tinks.tink.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.ui.components.AppSnackbarBus
import app.tinks.tink.zi.ZiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val storyLength: Int = 200,
    val showStoryLengthDialog: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ziRepository: ZiRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val uiState = _state.map { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value)

    init {
        loadStoryLength()
    }

    fun openStoryLengthDialog() {
        _state.update { it.copy(showStoryLengthDialog = true) }
    }

    fun dismissStoryLengthDialog() {
        _state.update { it.copy(showStoryLengthDialog = false) }
    }

    fun updateStoryLength(length: Int) {
        if (length <= 0) {
            AppSnackbarBus.showMessage("请输入有效长度")
            return
        }
        viewModelScope.launch {
            ziRepository.setStoryLength(length)
            _state.update {
                it.copy(
                    storyLength = length,
                    showStoryLengthDialog = false
                )
            }
            AppSnackbarBus.showMessage("设置已保存")
        }
    }

    private fun loadStoryLength() {
        viewModelScope.launch {
            val length = ziRepository.getStoryLength()
            _state.update { it.copy(storyLength = length) }
        }
    }
}
