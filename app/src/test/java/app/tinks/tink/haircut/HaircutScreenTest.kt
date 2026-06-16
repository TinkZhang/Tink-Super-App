package app.tinks.tink.haircut

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.tinks.tink.haircut.data.Haircut
import app.tinks.tink.ui.theme.TinkTheme
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class HaircutScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboard_rendersFreshStatusAndHistory() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                HaircutScreen(
                    days = 10,
                    history = listOf(Haircut(1, 35, LocalDate(2026, 5, 22), "Tink Cuts")),
                    isLoading = false,
                    showDialog = false,
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithText("10").assertIsDisplayed()
        composeRule.onNodeWithText("发型保持得不错").assertIsDisplayed()
        composeRule.onNodeWithText("理发历史").assertIsDisplayed()
        composeRule.onNodeWithText("Tink Cuts").assertIsDisplayed()
        composeRule.onNodeWithText("¥35").assertIsDisplayed()
    }

    @Test
    fun dashboard_rendersWarningStatus_whenDaysAreHigh() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                HaircutScreen(days = 40, history = emptyList())
            }
        }

        composeRule.onNodeWithText("该去剪头了！").assertIsDisplayed()
    }

    @Test
    fun fabClick_requestsAddDialog() {
        val events = mutableListOf<HaircutEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                HaircutScreen(days = 5, history = emptyList(), onEvent = events::add)
            }
        }

        composeRule.onNodeWithContentDescription("添加记录").performClick()

        assertTrue(events.contains(HaircutEvent.AddHaircutFabClick))
    }

    @Test
    fun addDialog_submitsValidHaircut() {
        val events = mutableListOf<HaircutEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                HaircutScreen(
                    days = 5,
                    history = emptyList(),
                    showDialog = true,
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithText("理发店名称").performClick()
        composeRule.onNodeWithText("理发店名称").performTextInput("Tink Cuts")
        composeRule.onNodeWithText("价格（元）").performClick()
        composeRule.onNodeWithText("价格（元）").performTextInput("45")
        composeRule.onNodeWithText("确定").performClick()

        val submit = events.single { it is HaircutEvent.SubmitHaircut } as HaircutEvent.SubmitHaircut
        assertEquals(45, submit.price)
        assertEquals("Tink Cuts", submit.shopName)
    }
}
