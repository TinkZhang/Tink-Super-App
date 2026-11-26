package app.tinks.tink.testing

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.tinks.tink.ui.theme.TinkTheme
import app.tinks.tink.zi.HandwritingState
import app.tinks.tink.zi.InkModelState
import app.tinks.tink.zi.WrittenZiListState
import app.tinks.tink.zi.ZiDashboard
import app.tinks.tink.zi.ZiHandwritingScreen
import app.tinks.tink.zi.ZiUiState
import app.tinks.tink.zi.Zi
import com.android.tools.screenshot.PreviewTest
import kotlinx.datetime.LocalDate

@PreviewTest
@Preview(widthDp = 320, heightDp = 640, locale = "zh")
@Composable
fun ZiDashboardPreview() {
    TinkTheme(dynamicColor = false) {
        Surface {
            ZiDashboard(
                uiState = ZiUiState(
                    isLoading = false,
                    showDialog = false,
                    learntZiNum = 126,
                    reviewList = listOf("春", "山", "书", "云", "光", "月").map {
                        Zi(it, LocalDate(2026, 1, 1), 2)
                    },
                ),
                written = WrittenZiListState(listOf("土", "山"), isLoading = false),
            )
        }
    }
}

@PreviewTest
@Preview(widthDp = 320, heightDp = 640, locale = "zh")
@Composable
fun ZiHandwritingCandidatePreview() {
    TinkTheme(dynamicColor = false) {
        Surface {
            ZiHandwritingScreen(
                state = HandwritingState(
                    model = InkModelState.Ready,
                    candidates = listOf("土", "士", "王"),
                    queuedCharacters = listOf("山", "木", "水", "火"),
                ),
            )
        }
    }
}
