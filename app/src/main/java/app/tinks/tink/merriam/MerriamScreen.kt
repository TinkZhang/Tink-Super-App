package app.tinks.tink.merriam


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
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
private fun MerriamScreen(
    units: List<app.tinks.tink.merriam.data.Unit>,
    weeklyRecords: List<DailyRecord>,
    latest: Int,
    isLoading: Boolean = false,
    onEvent: (MerriamEvent) -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val state = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { onEvent(MerriamEvent.Refresh) },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
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
                val coroutineScope = rememberCoroutineScope()
                LaunchedEffect(latest) {
                        listState.animateScrollToItem(index = max(0, (latest/10) -1))
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    state = listState,
                ) {
                    items(
                        items = units,
                        key = { it.id },
                        contentType = { it.roots.any { root -> root.id < latest } },
                    ) {
                        UnitItem(
                            unit = it,
                            latest = latest,
                            onRootComplete = { id, root ->
                                onEvent(
                                    MerriamEvent.CompleteRoot(
                                        id,
                                        root
                                    )
                                )
                            },
                        )
                    }
                }
            }

            WeeklyRecordMap(
                data = WeeklyRecordData(hasWeekSummary = null, records = weeklyRecords),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
            )
        }
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