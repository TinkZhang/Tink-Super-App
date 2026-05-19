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
import app.tinks.tink.weight.TrendChartCardUiState
import app.tinks.tink.weight.WeightControlCardUiState
import app.tinks.tink.weight.WeightScreen
import app.tinks.tink.weight.WeightUiState
import app.tinks.tink.weight.data.Weight
import com.android.tools.screenshot.PreviewTest

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
