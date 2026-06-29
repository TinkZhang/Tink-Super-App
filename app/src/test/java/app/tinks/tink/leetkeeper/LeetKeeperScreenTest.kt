package app.tinks.tink.leetkeeper

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
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
class LeetKeeperScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboard_rendersOngoingPlanAndProblems() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                LeetKeeperScreen(state = sampleLeetKeeperState())
            }
        }

        composeRule.onNodeWithText("LeetKeeper").assertIsDisplayed()
        composeRule.onNodeWithText("NeetCode 150").assertIsDisplayed()
        composeRule.onNodeWithText("2. Add Two Numbers").assertIsDisplayed()
    }

    @Test
    fun publicPlanClick_dispatchesSelectEvent() {
        val events = mutableListOf<LeetKeeperEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                LeetKeeperScreen(
                    state = sampleLeetKeeperState().copy(selectedTab = LeetKeeperTab.Popular),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithTag("leetkeeper_public_plan_1").performClick()

        assertEquals(listOf(LeetKeeperEvent.SelectPublicPlan(1)), events)
    }

    @Test
    fun problemDetail_rendersLongHtmlBody() {
        val body = buildString {
            append("<p>Given two strings, merge them in alternating order.</p>")
            repeat(60) { index ->
                append("<p>Example ")
                append(index)
                append(": keep appending the next available character.</p>")
            }
        }

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                LeetKeeperScreen(
                    state = sampleLeetKeeperState().copy(
                        focusedProblem = LeetKeeperProblemDetail(
                            id = "1768",
                            title = "Merge Strings Alternately",
                            details = body,
                            difficulty = LeetKeeperDifficulty.Easy,
                            link = "Merge-Strings-Alternately",
                            transactions = emptyList(),
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("leetkeeper_problem_detail").assertIsDisplayed()
        composeRule.onNodeWithText("1768. Merge Strings Alternately").assertIsDisplayed()
    }

    @Test
    fun completionSheetConfirm_dispatchesConfirmEvent() {
        val events = mutableListOf<LeetKeeperEvent>()
        val problem = sampleLeetKeeperState().ongoingPlan!!.modules.single().problems.last()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                LeetKeeperScreen(
                    state = sampleLeetKeeperState().copy(
                        completionProblem = problem,
                        durationText = "25",
                        submissionText = "12345",
                    ),
                    onEvent = events::add,
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("leetkeeper_completion_confirm").performClick()

        assertEquals(LeetKeeperEvent.ConfirmCompletion, events.last())
    }
}
