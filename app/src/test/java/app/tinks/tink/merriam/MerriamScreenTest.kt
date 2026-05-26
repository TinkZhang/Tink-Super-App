package app.tinks.tink.merriam

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import app.tinks.tink.merriam.data.Root
import app.tinks.tink.merriam.data.Unit
import app.tinks.tink.ui.theme.TinkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MerriamScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screen_rendersSummaryAndUnits() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                MerriamScreen(
                    units = listOf(sampleUnit()),
                    weeklyRecords = emptyList(),
                    latest = 11,
                )
            }
        }

        composeRule.onNodeWithTag("merriam_summary_card").assertIsDisplayed()
        composeRule.onNodeWithText("M-W Builder").assertIsDisplayed()
        composeRule.onNodeWithText("Latest root").assertIsDisplayed()
        composeRule.onNodeWithText("11").assertIsDisplayed()
        composeRule.onNodeWithTag("merriam_unit_1").assertIsDisplayed()
    }

    @Test
    fun longClickRoot_invokesCompleteRoot() {
        val completedIds = mutableListOf<Int>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                MerriamScreen(
                    units = listOf(sampleUnit(isExpanded = true)),
                    weeklyRecords = emptyList(),
                    latest = 11,
                    onEvent = {
                        if (it is MerriamEvent.CompleteRoot) {
                            completedIds.add(it.id)
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithTag("merriam_builder_list")
            .performScrollToNode(hasTestTag("merriam_root_12"))
        composeRule.onNodeWithTag("merriam_root_12")
            .performTouchInput { longClick() }

        assertEquals(listOf(12), completedIds)
    }

    @Test
    fun collapsedUnit_canBeExpanded() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                MerriamScreen(
                    units = listOf(sampleUnit(isExpanded = false)),
                    weeklyRecords = emptyList(),
                    latest = 10,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Toggle unit 1")
            .performClick()

        composeRule.onNodeWithText("Well").assertIsDisplayed()
    }

    private fun sampleUnit(isExpanded: Boolean = false): Unit = Unit(
        id = 1,
        roots = listOf(
            Root(
                id = 11,
                unit = 1,
                text = "BENE",
                meaning = "Well",
                words = listOf("benefit", "benefactor"),
            ),
            Root(
                id = 12,
                unit = 1,
                text = "AM",
                meaning = "To love",
                words = listOf("amicable", "amorous"),
            ),
        ),
        isExpanded = isExpanded,
    )
}
