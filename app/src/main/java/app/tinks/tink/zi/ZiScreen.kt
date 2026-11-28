package app.tinks.tink.zi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import app.tinks.tink.zi.game.ZiTestGameEntryButton

@Composable
fun ZiScreen(
    viewModel: ZiViewModel,
    onReadList: () -> Unit,
    onWrittenList: () -> Unit,
    onAddWritten: () -> Unit,
    onOpenTestGame: () -> Unit,
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
        onOpenTestGame = onOpenTestGame,
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
    onOpenTestGame: () -> Unit = {},
    onRetryWritten: () -> Unit = {},
    onStoryListNavigation: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize().testTag("zi_dashboard")) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading || written.isLoading,
            onRefresh = { onEvent(ZiEvent.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val viewportHeight = maxHeight
                val colors = MaterialTheme.colorScheme
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = viewportHeight)
                        .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ZiCountCard(
                            title = "会读汉字",
                            count = uiState.learntZiNum.takeUnless {
                                uiState.allZis.isEmpty() && (uiState.isLoading || uiState.isNetworkError)
                            },
                            addLabel = "添加会读的字",
                            id = "read",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                            actionColor = colors.primary,
                            onActionColor = colors.onPrimary,
                            onOpen = onReadList,
                            onAdd = { onEvent(ZiEvent.AddZiDialogOpen) },
                        )
                        ZiCountCard(
                            title = "会写汉字",
                            count = written.characters?.size,
                            addLabel = "添加会写的字",
                            id = "written",
                            icon = Icons.Filled.Edit,
                            containerColor = colors.tertiaryContainer,
                            contentColor = colors.onTertiaryContainer,
                            actionColor = colors.tertiary,
                            onActionColor = colors.onTertiary,
                            onOpen = onWrittenList,
                            onAdd = onAddWritten,
                        )
                        if (written.failed) {
                            TextButton(onClick = onRetryWritten) { Text("会写汉字加载失败，点击重试") }
                        }
                        ZiTestGameEntryButton(
                            readableCount = uiState.learntZiNum.takeUnless {
                                uiState.allZis.isEmpty() && (uiState.isLoading || uiState.isNetworkError)
                            },
                            onClick = onOpenTestGame,
                        )
                    }

                    if (uiState.reviewList.isNotEmpty()) {
                        ZiReviewCard(uiState.reviewList, Modifier.padding(vertical = 12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = if (uiState.reviewList.isEmpty()) 12.dp else 0.dp)
                            .testTag("zi_story_actions"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onStoryListNavigation,
                            modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("故事列表", textAlign = TextAlign.Center)
                        }
                        FilledTonalButton(
                            onClick = { onEvent(ZiEvent.GenerateStory) },
                            modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("生成故事", textAlign = TextAlign.Center)
                        }
                    }
                }
            }
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
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    actionColor: Color,
    onActionColor: Color,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().testTag("zi_${id}_card"),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                contentColor = actionColor,
            ) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = count?.toString() ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("zi_${id}_count"),
                )
            }
            FilledIconButton(
                onClick = onAdd,
                modifier = Modifier.testTag("zi_add_$id"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = actionColor,
                    contentColor = onActionColor,
                ),
            ) {
                Icon(Icons.Filled.Add, contentDescription = addLabel)
            }
        }
    }
}

@Composable
private fun ZiReviewCard(characters: List<Zi>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth().testTag("zi_review_card"),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainer,
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("复习汉字", style = MaterialTheme.typography.titleMedium)
            for (row in characters.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (character in row) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.outlineVariant),
                        ) {
                            Box(Modifier.heightIn(min = 56.dp).padding(4.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = character.zi,
                                    style = MaterialTheme.typography.headlineLarge,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
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
