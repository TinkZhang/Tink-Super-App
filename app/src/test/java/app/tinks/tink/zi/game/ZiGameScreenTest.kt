package app.tinks.tink.zi.game

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tinks.tink.ui.theme.TinkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class ZiGameScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun lobbyDisplaysSolarSystemStageAndStartsGame() {
        val actions = mutableListOf<String>()
        compose.setContent {
            TinkTheme(dynamicColor = false) {
                ZiGameScreen(
                    state = ZiGameUiState(
                        phase = ZiGamePhase.Lobby,
                        selectedStage = ZiGameLevels.stageForNumber(1),
                        highestUnlockedStage = 3,
                        readablePoolSize = 40,
                        totalCharsInStage = 20,
                        isLoadingPool = false,
                        hasMicPermission = true,
                        modelState = ZiVoiceModelState.Ready,
                    ),
                    onSelectStage = { actions += "stage:$it" },
                    onStartGame = { actions += "start" },
                )
            }
        }

        compose.onNodeWithText("第1关 · 水星").assertExists()
        compose.onNodeWithTag("zi_game_stage_2").performClick()
        compose.onNodeWithTag("zi_game_start_button").performClick()
        assertEquals(listOf("stage:2", "start"), actions)
    }

    @Test
    fun resultScreenAllowsTogglingAndSyncingMissedCharacters() {
        val actions = mutableListOf<String>()
        compose.setContent {
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
                        score = 1800,
                        maxCombo = 7,
                        missedChars = listOf(
                            ZiMissedChar("森", "sēn", selectedForReview = true),
                            ZiMissedChar("桥", "qiáo", selectedForReview = true),
                        ),
                    ),
                    onToggleMissedChar = { actions += "toggle:$it" },
                    onSyncMissedChars = { actions += "sync" },
                )
            }
        }

        compose.onNodeWithText("冥王星 探索完成！").assertExists()
        compose.onNodeWithTag("zi_game_missed_森").performClick()
        compose.onNodeWithTag("zi_game_sync_missed_button").performClick()
        assertEquals(listOf("toggle:森", "sync"), actions)
    }
}
