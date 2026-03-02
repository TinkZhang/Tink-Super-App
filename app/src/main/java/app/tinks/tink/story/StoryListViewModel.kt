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

data class StoryListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val stories: List<Story> = emptyList(),
    val deletingIds: Set<String> = emptySet(),
    val endReached: Boolean = false,
    val currentPage: Int = 0,
)

@HiltViewModel
class StoryListViewModel @Inject constructor(
    private val repository: StoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StoryListUiState())
    val uiState = _state.map { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value)

    private val pageSize = 10

    init {
        loadStories(refresh = true)
    }

    fun refresh() {
        loadStories(refresh = true)
    }

    fun loadMore() {
        val state = _state.value
        if (state.isLoading || state.isRefreshing || state.isLoadingMore || state.endReached) return
        loadStories(page = state.currentPage + 1, refresh = false)
    }

    private fun loadStories(page: Int = 0, refresh: Boolean) {
        repository.getStoryList(page = page, size = pageSize).onEach { result ->
            when (result) {
                is ApiResult.Loading -> _state.update {
                    it.copy(
                        isLoading = refresh && it.stories.isEmpty(),
                        isRefreshing = refresh && it.stories.isNotEmpty(),
                        isLoadingMore = !refresh
                    )
                }
                is ApiResult.Success -> _state.update {
                    val newStories = if (refresh) {
                        result.data
                    } else {
                        it.stories + result.data
                    }
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        stories = newStories,
                        currentPage = page,
                        endReached = result.data.isEmpty()
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false
                    )
                }.also {
                    AppSnackbarBus.showMessage("故事列表加载失败")
                }
            }
        }.launchIn(viewModelScope)
    }

    fun deleteStory(storyId: String) {
        if (storyId in _state.value.deletingIds) return
        val previousStories = _state.value.stories
        _state.update {
            it.copy(
                deletingIds = it.deletingIds + storyId,
                stories = it.stories.filterNot { story -> story.id == storyId }
            )
        }
        repository.deleteStory(storyId).onEach { result ->
            when (result) {
                is ApiResult.Loading -> Unit
                is ApiResult.Success -> _state.update {
                    it.copy(deletingIds = it.deletingIds - storyId)
                }.also {
                    AppSnackbarBus.showMessage("删除成功")
                }
                is ApiResult.Error -> _state.update {
                    it.copy(
                        deletingIds = it.deletingIds - storyId,
                        stories = previousStories
                    )
                }.also {
                    AppSnackbarBus.showMessage("删除失败")
                }
            }
        }.launchIn(viewModelScope)
    }
}
