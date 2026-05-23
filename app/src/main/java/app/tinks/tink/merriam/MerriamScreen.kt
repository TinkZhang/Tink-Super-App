package app.tinks.tink.merriam


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.merriam.data.Root
import app.tinks.tink.merriam.ui.UnitItem
import app.tinks.tink.ui.components.DailyRecord
import app.tinks.tink.ui.components.WeeklyRecordData
import app.tinks.tink.ui.components.WeeklyRecordMap
import app.tinks.tink.ui.theme.TinkTheme
import kotlin.math.max

@Composable
fun MerriamScreen(viewModel: MerriamViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MerriamScreen(
        units = uiState.units,
        weeklyRecords = uiState.weeklyRecords.records,
        latest = uiState.latest,
        isLoading = uiState.isLoading,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MerriamScreen(
    units: List<app.tinks.tink.merriam.data.Unit>,
    weeklyRecords: List<DailyRecord>,
    latest: Int,
    isLoading: Boolean = false,
    onEvent: (MerriamEvent) -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val state = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { onEvent(MerriamEvent.Refresh) },
            modifier = Modifier.fillMaxSize(),
            state = state,
            indicator = {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            val scale = if (isLoading) 1f else state.distanceFraction.coerceIn(0f, 1f)
                            scaleX = scale
                            scaleY = scale
                            alpha = scale
                        }
                ) {
                    ContainedLoadingIndicator()
                }
            }
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(latest) {
                listState.animateScrollToItem(index = max(0, (latest / 10) - 1))
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("merriam_builder_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                state = listState,
            ) {
                item {
                    MerriamSummaryCard(
                        latest = latest,
                        unitCount = units.size,
                        weeklyRecords = weeklyRecords,
                    )
                }

                items(
                    items = units,
                    key = { it.id },
                    contentType = { it.roots.any { root -> root.id < latest } },
                ) {
                    UnitItem(
                        unit = it,
                        latest = latest,
                        onRootComplete = { id -> onEvent(MerriamEvent.CompleteRoot(id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MerriamSummaryCard(
    latest: Int,
    unitCount: Int,
    weeklyRecords: List<DailyRecord>,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("merriam_summary_card"),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "M-W Builder",
                style = MaterialTheme.typography.titleLarge,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetric(
                    label = "Latest root",
                    value = if (latest > 0) latest.toString() else "--",
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "Units",
                    value = unitCount.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
            WeeklyRecordMap(
                data = WeeklyRecordData(hasWeekSummary = null, records = weeklyRecords),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@PreviewLightDark
@Composable
private fun MerriamScreenPreview() {
    TinkTheme {
        MerriamScreen(
            units = listOf(
                _root_ide_package_.app.tinks.tink.merriam.data.Unit(
                    id = 1,
                    roots = listOf(
                        Root(
                            unit = 1,
                            id = 11,
                            text = "BENE",
                            meaning = "Well",
                            words = listOf(
                                "benediction",
                                "benefactor",
                                "beneficiary",
                                "benevolence"
                            )
                        ),
                        Root(
                            unit = 1,
                            id = 12,
                            text = "AM",
                            meaning = "To love",
                            words = listOf("amicable", "enamored", "amorous", "paramour"),
                        ),
                    ),
                ),
                _root_ide_package_.app.tinks.tink.merriam.data.Unit(
                    id = 2,
                    roots = listOf(
                        Root(
                            unit = 1,
                            id = 11,
                            text = "BENE",
                            meaning = "Well",
                            words = listOf(
                                "benediction",
                                "benefactor",
                                "beneficiary",
                                "benevolence"
                            )
                        ),
                        Root(
                            unit = 1,
                            id = 12,
                            text = "AM",
                            meaning = "To love",
                            words = listOf("amicable", "enamored", "amorous", "paramour"),
                        ),
                    ),
                    isExpanded = true
                ),
            ),
            weeklyRecords = listOf(),
            latest = 11,
        )
    }
}
