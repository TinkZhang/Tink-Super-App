package app.tinks.tink.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StoryDetailScreen(
    viewModel: StoryDetailViewModel,
    storyId: String,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(storyId) {
        viewModel.loadStory(storyId)
    }
    StoryDetailScreen(
        story = uiState.story,
        isLoading = uiState.isLoading,
        onRefresh = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StoryDetailScreen(
    story: Story?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = state,
        indicator = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val scale = if (isLoading) 1f else state.distanceFraction.coerceIn(0f, 1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = scale
                    },
                verticalArrangement = Arrangement.Center
            ) {
                ContainedLoadingIndicator()
            }
        }
    ) {
        if (story == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text("加载中...", style = MaterialTheme.typography.bodyLarge)
            }
            return@PullToRefreshBox
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = story.title,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "创建时间：${story.createdAt}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "长度：${story.length}，唯一字数：${story.uniqueChar}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = story.content ?: "暂无内容",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
