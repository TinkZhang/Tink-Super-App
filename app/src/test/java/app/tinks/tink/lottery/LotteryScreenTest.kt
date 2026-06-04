package app.tinks.tink.lottery

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class LotteryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboard_invokesCameraAndHistoryCallbacks() {
        var cameraClicks = 0
        var historyClicks = 0

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                LotteryScreen(
                    state = sampleUiState(),
                    onOpenHistoryStats = { historyClicks += 1 },
                    onRequestCapture = { cameraClicks += 1 },
                )
            }
        }

        composeRule.onNodeWithTag("lottery_add_fab").performClick()
        composeRule.onNodeWithTag("lottery_history_stats_button").performClick()

        assertEquals(1, cameraClicks)
        assertEquals(1, historyClicks)
    }

    @Test
    fun readyTicket_invokesRevealAction() {
        val events = mutableListOf<LotteryEvent>()
        val ticket = sampleTicketUiState(id = 9, status = LotteryTicketStatus.Ready)

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                LotteryScreen(
                    state = sampleUiState(active = ticket),
                    onEvent = { events.add(it) },
                )
            }
        }

        composeRule.onNodeWithTag("lottery_reveal_button_9").performClick()

        assertEquals(listOf(LotteryEvent.CheckLottery(9)), events)
    }

    @Test
    fun reviewScreen_rendersJsonAndSendsSaveEvent() {
        val events = mutableListOf<LotteryEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                LotteryScreen(
                    state = sampleUiState(draft = LotteryDraft(
                        issueId = "21126",
                        frontNumbersText = "01 11 12 34 35",
                        backNumbersText = "09 12",
                        revealTimeText = "2021-11-03T12:30:00Z",
                    )),
                    onEvent = { events.add(it) },
                )
            }
        }

        composeRule.onNodeWithTag("lottery_review_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Review ticket").assertIsDisplayed()
        composeRule.onNodeWithTag("lottery_review_screen")
            .performScrollToNode(hasTestTag("lottery_review_save_button"))
        composeRule.onNodeWithTag("lottery_review_save_button").performClick()

        assertTrue(events.contains(LotteryEvent.SaveDraft))
    }

    @Test
    fun hero_rendersWinningResult() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                LotteryScreen(
                    state = sampleUiState(luckyOutcome = sampleOutcome()),
                )
            }
        }

        composeRule.onNodeWithTag("lottery_result_hero").assertIsDisplayed()
        composeRule.onNodeWithText("一等奖").assertIsDisplayed()
    }

    @Test
    fun historyStats_rendersStatsAndDeleteAction() {
        val events = mutableListOf<LotteryEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                LotteryHistoryStatsScreen(
                    state = sampleUiState(),
                    onEvent = { events.add(it) },
                )
            }
        }

        composeRule.onNodeWithTag("lottery_stats_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("lottery_history_stats_screen")
            .performScrollToNode(hasTestTag("lottery_ticket_card_1"))
        composeRule.onNodeWithContentDescription("Delete lottery").performClick()

        assertEquals(listOf(LotteryEvent.DeleteLottery(1)), events)
    }

    private fun sampleUiState(
        active: LotteryTicketUiState = sampleTicketUiState(),
        draft: LotteryDraft? = null,
        luckyOutcome: LotteryCheckOutcome? = null,
    ): LotteryUiState =
        LotteryUiState(
            isLoading = false,
            activeTicket = active,
            historyTickets = listOf(active),
            stats = LotteryStatsUiState(
                totalTickets = 1,
                pendingTickets = 0,
                revealedTickets = if (active.status == LotteryTicketStatus.Revealed) 1 else 0,
                winningTickets = if (active.matchSummary?.isWinning == true) 1 else 0,
                bestPrizeTier = active.matchSummary?.prizeTier ?: "暂无",
                prizeDistribution = active.matchSummary?.let { listOf(it.prizeTier to 1) } ?: emptyList(),
            ),
            draft = draft,
            luckyOutcome = luckyOutcome,
        )

    private fun sampleTicketUiState(
        id: Int = 1,
        status: LotteryTicketStatus = LotteryTicketStatus.Ready,
    ): LotteryTicketUiState =
        LotteryTicketUiState(
            ticket = sampleTicket(id = id, checked = status == LotteryTicketStatus.Revealed),
            status = status,
            revealTimeText = "2021-11-03 12:30:00",
            checkedTimeText = null,
            matchSummary = if (status == LotteryTicketStatus.Revealed) {
                LotteryMatchSummary(5, 2, "一等奖")
            } else {
                null
            },
        )

    private fun sampleOutcome(): LotteryCheckOutcome {
        val ticket = sampleTicket(id = 1, checked = true)
        val result = LotteryResult(
            id = 4,
            type = LOTTERY_TYPE_DA_LE_TOU,
            issueId = "21126",
            numbers = LotteryNumbers(listOf(1, 11, 12, 34, 35), listOf(9, 12)),
            openedAt = Instant.parse("2021-11-03T12:30:00Z"),
            source = "mxnzp",
        )
        return LotteryCheckOutcome(ticket, result, 5, 2, "一等奖")
    }

    private fun sampleTicket(id: Int, checked: Boolean = false): LotteryTicket =
        LotteryTicket(
            id = id,
            type = LOTTERY_TYPE_DA_LE_TOU,
            issueId = "21126",
            numbers = LotteryNumbers(listOf(1, 11, 12, 34, 35), listOf(9, 12)),
            revealTime = Instant.parse("2021-11-03T12:30:00Z"),
            capturedImageUri = null,
            checked = checked,
            checkedAt = null,
            resultId = null,
            prizeTier = if (checked) "一等奖" else null,
            frontMatchCount = if (checked) 5 else null,
            backMatchCount = if (checked) 2 else null,
            result = null,
        )
}
