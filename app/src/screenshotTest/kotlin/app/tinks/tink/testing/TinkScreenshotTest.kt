package app.tinks.tink.testing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.tinks.tink.ui.theme.TinkTheme
import app.tinks.tink.time.DEFAULT_TIME_TYPE
import app.tinks.tink.time.TimeDayEntries
import app.tinks.tink.time.TimeEntry
import app.tinks.tink.time.TimeEditorState
import app.tinks.tink.time.TimeLabel
import app.tinks.tink.time.TimeLabelManagerState
import app.tinks.tink.time.TimeScreen
import app.tinks.tink.time.TimeStatistic
import app.tinks.tink.time.TimeUiState
import app.tinks.tink.weight.TrendChartCardUiState
import app.tinks.tink.weight.WeightControlCardUiState
import app.tinks.tink.weight.WeightScreen
import app.tinks.tink.weight.WeightUiState
import app.tinks.tink.weight.data.Weight
import com.android.tools.screenshot.PreviewTest
import java.time.LocalDate
import java.time.OffsetDateTime

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 500)
@Composable
fun TinkScreenshotSmokePreview() {
    TinkTheme(dynamicColor = false) {
        Surface {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Tink test surface")
                Button(onClick = {}) {
                    Text("Ready")
                }
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
fun WeightOverviewScreenshotPreview() {
    TinkTheme(dynamicColor = false) {
        WeightScreen(
            state = WeightUiState(
                isLoading = false,
                weightControlCardUiState = WeightControlCardUiState(
                    isTodayRecorded = false,
                    lastDateText = "2026-05-18",
                    showConfirm = false,
                    newWeight = 141.2,
                ),
                trendChartCardUiState = TrendChartCardUiState(
                    selectedIndex = 0,
                    weightList = listOf(
                        Weight(id = 1, weight = 142.0, createdTime = 1778745600000L),
                        Weight(id = 2, weight = 141.6, createdTime = 1778918400000L),
                        Weight(id = 3, weight = 141.2, createdTime = 1779177600000L),
                    ),
                ),
                allWeights = listOf(
                    Weight(id = 3, weight = 141.2, createdTime = 1779177600000L),
                    Weight(id = 2, weight = 141.6, createdTime = 1778918400000L),
                    Weight(id = 1, weight = 142.0, createdTime = 1778745600000L),
                ),
            ),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
fun TimeDashboardScreenshotPreview() {
    TinkTheme(dynamicColor = false) {
        TimeScreen(
            state = TimeUiState(
                isLoading = false,
                isSaving = false,
                startDate = LocalDate.parse("2026-05-22"),
                endDate = LocalDate.parse("2026-05-22"),
                statistics = listOf(
                    TimeStatistic(type = 5, duration = 210),
                    TimeStatistic(type = 9, duration = 90),
                    TimeStatistic(type = 2, duration = 45),
                ),
                entriesByDay = listOf(
                    TimeDayEntries(
                        day = LocalDate.parse("2026-05-22"),
                        entries = listOf(
                            TimeEntry(
                                id = 1,
                                type = 5,
                                start = OffsetDateTime.parse("2026-05-22T09:00:00+08:00"),
                                end = OffsetDateTime.parse("2026-05-22T11:30:00+08:00"),
                                title = "Planning: Weekly roadmap",
                                description = "Time feature polish",
                            ),
                            TimeEntry(
                                id = 2,
                                type = 9,
                                start = OffsetDateTime.parse("2026-05-22T14:00:00+08:00"),
                                end = OffsetDateTime.parse("2026-05-22T15:30:00+08:00"),
                                title = "Coding: Appium tests",
                                description = null,
                            ),
                        ),
                    )
                ),
                deletingIds = emptySet(),
                showEditor = false,
                editor = TimeEditorState.defaultNow(),
                labels = listOf(
                    TimeLabel(id = 1, type = DEFAULT_TIME_TYPE, name = "Planning", sortOrder = 0),
                    TimeLabel(id = 2, type = DEFAULT_TIME_TYPE, name = "Coding", sortOrder = 1),
                ),
                showLabelManager = false,
                labelManager = TimeLabelManagerState(),
            ),
            onEvent = {},
        )
    }
}
