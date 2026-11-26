package app.tinks.tink.zi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.ui.components.ContentCard

@Composable
fun ZiScreen(
    viewModel: ZiViewModel,
    onReadList: () -> Unit,
    onWrittenList: () -> Unit,
    onAddWritten: () -> Unit,
    onStoryListNavigation: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val written by viewModel.writtenState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshWritten() }
    ZiDashboard(
        uiState = uiState,
        written = written,
        onEvent = viewModel::onEvent,
        onReadList = onReadList,
        onWrittenList = onWrittenList,
        onAddWritten = onAddWritten,
        onRetryWritten = viewModel::refreshWritten,
        onStoryListNavigation = onStoryListNavigation,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ZiDashboard(
    uiState: ZiUiState,
    written: WrittenZiListState,
    onEvent: (ZiEvent) -> Unit = {},
    onReadList: () -> Unit = {},
    onWrittenList: () -> Unit = {},
    onAddWritten: () -> Unit = {},
    onRetryWritten: () -> Unit = {},
    onStoryListNavigation: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize().testTag("zi_dashboard")) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading || written.isLoading,
            onRefresh = { onEvent(ZiEvent.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ZiCountCard(
                        title = "会读汉字数量",
                        count = uiState.learntZiNum.takeUnless {
                            uiState.allZis.isEmpty() && (uiState.isLoading || uiState.isNetworkError)
                        },
                        addLabel = "添加会读的字", id = "read",
                        onOpen = onReadList, onAdd = { onEvent(ZiEvent.AddZiDialogOpen) },
                    )
                }
                item {
                    ZiCountCard(
                        title = "会写汉字数量", count = written.characters?.size,
                        addLabel = "添加会写的字", id = "written",
                        onOpen = onWrittenList, onAdd = onAddWritten,
                    )
                }
                if (written.failed) {
                    item { TextButton(onClick = onRetryWritten) { Text("会写汉字加载失败，点击重试") } }
                }
                if (uiState.reviewList.isNotEmpty()) {
                    item {
                        ContentCard(title = "复习汉字") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.reviewList.chunked(3).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { zi ->
                                            SawtoothCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp)) {
                                                Text(
                                                    text = zi.zi,
                                                    style = MaterialTheme.typography.headlineLarge,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            }
                                        }
                                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onStoryListNavigation,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "故事列表")
        }
        FloatingActionButton(
            onClick = { onEvent(ZiEvent.GenerateStory) },
            shape = FloatingActionButtonDefaults.largeShape,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = "生成故事")
        }
        if (uiState.showDialog) {
            AddZiDialog(
                onDismiss = { onEvent(ZiEvent.DismissDialog) },
                onConfirm = { proficiency, zis -> onEvent(ZiEvent.UpdateZi(proficiency ?: 0, zis)) },
            )
        }
    }
}

@Composable
private fun ZiCountCard(
    title: String,
    count: Int?,
    addLabel: String,
    id: String,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth().testTag("zi_${id}_card")) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = count?.toString() ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("zi_${id}_count"),
                )
                FilledTonalIconButton(onClick = onAdd, modifier = Modifier.testTag("zi_add_$id")) {
                    Icon(Icons.Filled.Add, contentDescription = addLabel)
                }
            }
        }
    }
}

@Preview(widthDp = 320, heightDp = 640, locale = "zh")
@Composable
private fun ZiScreenPreview() {
    ZiDashboard(
        uiState = ZiUiState(isLoading = false, showDialog = false, learntZiNum = 126),
        written = WrittenZiListState(listOf("一", "土", "山"), isLoading = false),
    )
}
