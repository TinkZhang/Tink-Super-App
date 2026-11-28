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
import app.tinks.tink.zi.game.ZiFallingTile
import app.tinks.tink.zi.game.ZiGameLevels
import app.tinks.tink.zi.game.ZiGamePhase
import app.tinks.tink.zi.game.ZiGameScreen
import app.tinks.tink.zi.game.ZiGameUiState
import app.tinks.tink.zi.game.ZiMissedChar
import app.tinks.tink.zi.game.ZiVoiceModelState
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

@PreviewTest
@Preview(widthDp = 320, heightDp = 640, locale = "zh")
@Composable
fun ZiGameLobbyPreview() {
    TinkTheme(dynamicColor = false) {
        ZiGameScreen(
            state = ZiGameUiState(
                phase = ZiGamePhase.Lobby,
                selectedStage = ZiGameLevels.stageForNumber(3),
                highestUnlockedStage = 10,
                stageStars = mapOf(1 to 3, 2 to 2, 3 to 3),
                bestScore = 1850,
                readablePoolSize = 128,
                totalCharsInStage = 20,
                isLoadingPool = false,
                hasMicPermission = true,
                modelState = ZiVoiceModelState.Ready,
            ),
        )
    }
}

@PreviewTest
@Preview(widthDp = 320, heightDp = 640, locale = "zh")
@Composable
fun ZiGamePlayingPreview() {
    TinkTheme(dynamicColor = false) {
        ZiGameScreen(
            state = ZiGameUiState(
                phase = ZiGamePhase.Playing,
                selectedStage = ZiGameLevels.stageForNumber(6),
                totalCharsInStage = 20,
                completedCount = 4,
                hitCount = 4,
                score = 460,
                combo = 4,
                isListening = true,
                micLevel = 0.55f,
                currentTile = ZiFallingTile(zi = "木", pinyin = "mù", xFraction = 0.5f, yFraction = 0.42f),
            ),
        )
    }
}

@PreviewTest
@Preview(widthDp = 320, heightDp = 640, locale = "zh")
@Composable
fun ZiGameResultPreview() {
    TinkTheme(dynamicColor = false) {
        ZiGameScreen(
            state = ZiGameUiState(
                phase = ZiGamePhase.Result,
                selectedStage = ZiGameLevels.stageForNumber(9),
                highestUnlockedStage = 10,
                newlyUnlockedStage = ZiGameLevels.stageForNumber(10),
                totalCharsInStage = 20,
                completedCount = 20,
                hitCount = 18,
                score = 1920,
                maxCombo = 9,
                missedChars = listOf(
                    ZiMissedChar("森", "sēn", selectedForReview = true),
                    ZiMissedChar("桥", "qiáo", selectedForReview = true),
                ),
            ),
        )
    }
}
