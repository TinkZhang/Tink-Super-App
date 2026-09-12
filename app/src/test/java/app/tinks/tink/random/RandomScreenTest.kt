package app.tinks.tink.random

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import app.tinks.tink.ui.theme.TinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class RandomScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun settingsAreClean_andStartOnlyNavigates() {
        var starts = 0
        compose.setContent {
            var settings by remember { mutableStateOf(RandomSettings()) }
            TinkTheme(dynamicColor = false) { RandomSettingsScreen(settings, onChange = { settings = it }, onPlay = { starts++ }) }
        }
        compose.onNodeWithTag("random_dice_plus").performClick()
        compose.onNodeWithTag("random_dice_count").assertTextEquals("3")
        compose.onNodeWithText("游玩方式").assertDoesNotExist()
        compose.onNodeWithTag("random_start").performScrollTo().performClick()
        assertEquals(1, starts)
        compose.onNodeWithTag("random_play").assertDoesNotExist()
    }
    @Test fun playHasOnlyObjects_andIgnoresClicksWhileRolling() {
        var taps = 0
        var state by mutableStateOf(RandomPlayState())
        compose.setContent { TinkTheme(dynamicColor = false) { RandomPlayScreen(RandomSettings(), state, onRoll = { taps++ }) } }
        compose.onNodeWithText("中文播报").assertDoesNotExist()
        compose.onNodeWithTag("random_start").assertDoesNotExist()
        compose.onNodeWithTag("random_play_area").performClick()
        assertEquals(1, taps)
        compose.runOnIdle { state = state.copy(rolling = true, round = 1, durationMillis = 1500) }
        compose.onNodeWithTag("random_play_area").assertIsNotEnabled()
    }
    @Test fun wheelDraftSurvivesRecreation_andInvalidWeightsCannotSave() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TinkTheme(dynamicColor = false) { RandomWheelEditor(RandomSettings(type = RandomType.Wheel)) } }
        compose.onNodeWithTag("random_section_0_content").performTextReplacement("米饭")
        compose.onNodeWithTag("random_section_0_weight").performTextReplacement("0")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("random_section_0_content").assertTextContains("米饭")
        compose.onNodeWithTag("random_wheel_save").performScrollTo().assertIsNotEnabled()
    }
    @Test fun deletingSectionRequiresConfirmation() {
        compose.setContent { TinkTheme(dynamicColor = false) { RandomWheelEditor(RandomSettings(type = RandomType.Wheel)) } }
        compose.onNodeWithTag("random_section_0_delete").performClick()
        compose.onNodeWithText("删除“面条”？").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithTag("random_sections_count").assertTextEquals("4")
    }
}
