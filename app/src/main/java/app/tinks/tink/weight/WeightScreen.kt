package app.tinks.tink.weight

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.weight.data.Weight
import app.tinks.tink.weight.ui.TrendChartCard
import app.tinks.tink.weight.ui.WeightControlCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WeightScreen(viewModel: WeightViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeightScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WeightScreen(
    state: WeightUiState,
    onEvent: (WeightEvent) -> Unit = {},
) {
    val refreshState = rememberPullToRefreshState()
    var showHistory by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { onEvent(WeightEvent.RefreshWeightList) },
            modifier = Modifier.fillMaxSize(),
            state = refreshState,
            indicator = {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            val scale =
                                if (state.isLoading) 1f else refreshState.distanceFraction.coerceIn(
                                    0f,
                                    1f
                                )
                            scaleX = scale
                            scaleY = scale
                            alpha = scale
                        }
                ) {
                    ContainedLoadingIndicator()
                }
            }
        ) {
            AnimatedContent(
                targetState = showHistory,
                label = "weight_history_screen",
                transitionSpec = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(220)
                    ) + fadeIn(animationSpec = tween(220)) togetherWith
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(220)
                        ) + fadeOut(animationSpec = tween(220))
                },
            ) {
                if (it) {
                    WeightHistoryScreen(
                        weights = state.allWeights,
                        onBack = { showHistory = false },
                        onDelete = { id -> onEvent(WeightEvent.DeleteWeight(id)) },
                    )
                } else {
                    WeightDashboardScreen(
                        state = state,
                        onEvent = onEvent,
                        onOpenHistory = { showHistory = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightDashboardScreen(
    state: WeightUiState,
    onEvent: (WeightEvent) -> Unit,
    onOpenHistory: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
    ) {
        item("weight_control") {
            WeightControlCard(
                state.weightControlCardUiState,
                onEvent = onEvent,
            )
        }

        item("trend_chart") {
            TrendChartCard(
                state.trendChartCardUiState,
                onEvent = onEvent,
            )
        }

        item("history") {
            Button(
                onClick = onOpenHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            ) {
                Icon(Icons.Filled.History, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("查看所有历史记录")
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun WeightHistoryScreen(
    weights: List<Weight>,
    onBack: () -> Unit,
    onDelete: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
    ) {
        item("back_to_overview") {
            OutlinedButton(onClick = onBack) {
                Text("返回概览")
            }
        }

        if (weights.isEmpty()) {
            item("empty") {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.MonitorWeight, contentDescription = null)
                        Text("暂无体重记录", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "返回体重页添加第一条记录。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(weights, key = { it.id }) { weight ->
                WeightHistoryItem(weight = weight, onDelete = { onDelete(weight.id) })
            }
        }
    }
}

@Composable
private fun WeightHistoryItem(weight: Weight, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(
                    text = "%.1f 斤".format(weight.weight),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = { Text(weight.createdTime.toWeightDateText()) },
            leadingContent = {
                Icon(Icons.Outlined.MonitorWeight, contentDescription = null)
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        )
    }
}

private fun Long.toWeightDateText(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

@Preview
@Composable
private fun WeightScreenPreview() {
    WeightScreen(
        state = WeightUiState(
            isLoading = false,
            weightControlCardUiState = WeightControlCardUiState(
                isTodayRecorded = false,
                lastDateText = "2023-07-01",
                showConfirm = false,
                newWeight = null,
            ),
            trendChartCardUiState = TrendChartCardUiState(
                selectedIndex = 0,
                weightList = listOf(
                    Weight(id = 1, weight = 80.0, createdTime = 1688342400000),
                ),
            ),
            allWeights = listOf(
                Weight(id = 1, weight = 80.0, createdTime = 1688342400000),
            ),
        )
    )
}
