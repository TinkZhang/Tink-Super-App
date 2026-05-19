package app.tinks.tink.weight

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.tinks.tink.ui.theme.TinkTheme
import app.tinks.tink.weight.data.Weight
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WeightScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun overview_invokesOpenHistory_whenHistoryButtonClicked() {
        var openHistoryClicks = 0

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                WeightScreen(
                    state = sampleUiState(),
                    onOpenHistory = { openHistoryClicks += 1 },
                )
            }
        }

        composeRule.onNodeWithTag("weight_dashboard_list")
            .performScrollToNode(hasTestTag("weight_history_button"))

        composeRule.onNodeWithTag("weight_history_button")
            .performClick()

        assertEquals(1, openHistoryClicks)
    }

    @Test
    fun historyList_rendersWeightsAndInvokesDelete() {
        val deletedIds = mutableListOf<Int>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                WeightHistoryScreen(
                    weights = listOf(Weight(id = 7, weight = 141.2, createdTime = 1779178530000L)),
                    onDelete = { deletedIds.add(it) },
                )
            }
        }

        composeRule.onNodeWithText("141.2 斤").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("删除").performClick()

        assertEquals(listOf(7), deletedIds)
    }

    @Test
    fun historyList_rendersEmptyState() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                WeightHistoryScreen(weights = emptyList(), onDelete = {})
            }
        }

        composeRule.onNodeWithText("暂无体重记录").assertIsDisplayed()
    }

    private fun sampleUiState(): WeightUiState =
        WeightUiState(
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
                    Weight(id = 1, weight = 141.8, createdTime = 1779091200000L),
                    Weight(id = 2, weight = 141.2, createdTime = 1779177600000L),
                ),
            ),
            allWeights = listOf(
                Weight(id = 2, weight = 141.2, createdTime = 1779177600000L),
                Weight(id = 1, weight = 141.8, createdTime = 1779091200000L),
            ),
        )
}
