package app.tinks.tink.merriam


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.merriam.data.Root
import app.tinks.tink.merriam.ui.UnitItem
import app.tinks.tink.ui.components.DailyRecord
import app.tinks.tink.ui.components.WeeklyRecordData
import app.tinks.tink.ui.components.WeeklyRecordMap
import app.tinks.tink.ui.theme.TinkTheme
import kotlinx.datetime.LocalDate

@Composable
fun MerriamScreen(viewModel: MerriamViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MerriamScreen(
        units = uiState.units,
        weeklyRecords = uiState.weeklyRecords.records,
        isLoading = uiState.isLoading,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun MerriamScreen(
    units: List<app.tinks.tink.merriam.data.Unit>,
    weeklyRecords: List<DailyRecord>,
    isLoading: Boolean = false,
    showDialog: Boolean = false,
    onEvent: (MerriamEvent) -> Unit = {},
) {

    LaunchedEffect(Unit) {
//        onEvent(ZiEvent.Refresh)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            state = rememberLazyListState(),
        ) {
            items(
                items = units,
                key = { it.id },
                contentType = { it.roots.any { root -> !root.isCompleted } },
            ) {
                UnitItem(unit = it, onRootComplete = { onEvent(MerriamEvent.CompleteRoot(it)) })
            }
            item {
                Box(
                    modifier = Modifier
                        .height(64.dp)
                        .fillMaxWidth()
                        .background(Color.Green)
                ) {
                    WeeklyRecordMap(
                        data = WeeklyRecordData(hasWeekSummary = null, records = weeklyRecords),
                    )
                }
            }
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
                            isCompleted = true,
                            completeDate = LocalDate(year = 2026, month = 1, day = 26)
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
                            isCompleted = true,
                            completeDate = LocalDate(year = 2026, month = 1, day = 26)
                        ),
                    ),
                    isExpanded = true
                ),
            ),
            weeklyRecords = listOf()
        )
    }
}