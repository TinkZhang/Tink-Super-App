package app.tinks.tink.weight

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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
fun WeightScreen(
    viewModel: WeightViewModel,
    onOpenHistory: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeightScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onOpenHistory = onOpenHistory,
    )
}

@Composable
internal fun WeightScreen(
    state: WeightUiState,
    onEvent: (WeightEvent) -> Unit = {},
    onOpenHistory: () -> Unit = {},
) {
    val refreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { onEvent(WeightEvent.RefreshWeightList) },
            modifier = Modifier.fillMaxSize(),
            state = refreshState,
        ) {
            WeightDashboardScreen(
                state = state,
                onEvent = onEvent,
                onOpenHistory = onOpenHistory,
            )
        }
    }
}

@Composable
fun WeightHistoryScreen(viewModel: WeightViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeightHistoryScreen(
        weights = uiState.allWeights,
        onDelete = { id -> viewModel.onEvent(WeightEvent.DeleteWeight(id)) },
    )
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
            .padding(horizontal = 16.dp)
            .testTag("weight_dashboard_list"),
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
            FilledTonalButton(
                onClick = onOpenHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("weight_history_button")
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
internal fun WeightHistoryScreen(
    weights: List<Weight>,
    onDelete: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("weight_history_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
    ) {
        if (weights.isEmpty()) {
            item("empty") {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weight_history_empty")
                ) {
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
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weight_history_item_${weight.id}")
    ) {
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
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("weight_delete_button_${weight.id}")
                ) {
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
