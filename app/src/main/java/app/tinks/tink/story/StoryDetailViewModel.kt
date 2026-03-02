package app.tinks.tink.story

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
import javax.inject.Inject

data class StoryDetailUiState(
    val isLoading: Boolean = false,
    val story: Story? = null,
    val storyId: String? = null,
)

@HiltViewModel
class StoryDetailViewModel @Inject constructor(
    private val repository: StoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StoryDetailUiState())
    val uiState = _state.map { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value)

    fun loadStory(storyId: String) {
        _state.update { it.copy(storyId = storyId) }
        repository.getStory(storyId).onEach { result ->
            when (result) {
                is ApiResult.Loading -> _state.update { it.copy(isLoading = true) }
                is ApiResult.Success -> _state.update {
                    it.copy(isLoading = false, story = result.data)
                }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false) }.also {
                    AppSnackbarBus.showMessage("故事加载失败")
                }
            }
        }.launchIn(viewModelScope)
    }

    fun refresh() {
        val id = _state.value.storyId ?: return
        loadStory(id)
    }
}
