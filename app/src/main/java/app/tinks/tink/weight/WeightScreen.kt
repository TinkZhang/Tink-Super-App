package app.tinks.tink.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.weight.data.Weight
import app.tinks.tink.weight.ui.TrendChartCard
import app.tinks.tink.weight.ui.WeightControlCard

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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
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
                        onClick = { /* TODO: Navigate to full history */ },
                        modifier = Modifier.fillMaxWidth(),
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
    }
}

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
        )
    )
}
