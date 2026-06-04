package app.tinks.tink.lottery

import app.tinks.tink.network.ApiResult
import app.tinks.tink.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class LotteryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsHistoryAndBuildsActiveReadyTicket() = runTest {
        val ticket = sampleTicket(id = 1, revealTime = Instant.parse("2021-11-03T12:30:00Z"))
        val repository = FakeLotteryRepository(initialTickets = mutableListOf(ticket))

        val viewModel = LotteryViewModel(repository)

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(ticket.id, uiState.activeTicket?.ticket?.id)
        assertEquals(LotteryTicketStatus.Ready, uiState.activeTicket?.status)
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun capturePreview_opensEditableReviewDraft() = runTest {
        val viewModel = LotteryViewModel(FakeLotteryRepository())

        viewModel.onEvent(LotteryEvent.UseCapturedTicketPreview)

        val draft = viewModel.uiState.value.draft
        assertNotNull(draft)
        assertEquals("21126", draft?.issueId)
        assertEquals("01 11 12 34 35", draft?.frontNumbersText)
    }

    @Test
    fun saveDraft_postsLotteryAndRefreshesHistory() = runTest {
        val repository = FakeLotteryRepository()
        val viewModel = LotteryViewModel(repository)

        viewModel.onEvent(LotteryEvent.UseCapturedTicketPreview)
        viewModel.onEvent(LotteryEvent.SaveDraft)

        assertEquals("21126", repository.createdRequests.single().issueId)
        assertNull(viewModel.uiState.value.draft)
        assertEquals(2, repository.refreshCalls)
    }

    @Test
    fun invalidDraft_keepsReviewOpenWithError() = runTest {
        val viewModel = LotteryViewModel(FakeLotteryRepository())

        viewModel.onEvent(LotteryEvent.UseCapturedTicketPreview)
        viewModel.onEvent(LotteryEvent.UpdateDraftFrontNumbers("1 2"))
        viewModel.onEvent(LotteryEvent.SaveDraft)

        val draft = viewModel.uiState.value.draft
        assertNotNull(draft)
        assertTrue(draft?.parseError?.contains("Expected 5 numbers") == true)
    }

    @Test
    fun checkWinningLottery_updatesTicketAndShowsHero() = runTest {
        val ticket = sampleTicket(id = 8)
        val repository = FakeLotteryRepository(initialTickets = mutableListOf(ticket))
        val viewModel = LotteryViewModel(repository)

        viewModel.onEvent(LotteryEvent.CheckLottery(8))

        assertEquals(listOf(8), repository.checkedIds)
        assertEquals("一等奖", viewModel.uiState.value.activeTicket?.matchSummary?.prizeTier)
        assertEquals("一等奖", viewModel.uiState.value.luckyOutcome?.prizeTier)
    }

    @Test
    fun stateStats_mergeHistoryAndStats() {
        val state = LotteryState(
            tickets = listOf(
                sampleTicket(id = 1, checked = false, revealTime = Instant.parse("2026-12-01T12:30:00Z")),
                sampleTicket(id = 2, checked = true, prizeTier = "五等奖"),
                sampleTicket(id = 3, checked = true, prizeTier = "未中奖", frontMatches = 1, backMatches = 1),
            )
        )

        val uiState = state.toUiState(now = Instant.parse("2026-06-04T12:00:00Z"))

        assertEquals(3, uiState.stats.totalTickets)
        assertEquals(1, uiState.stats.pendingTickets)
        assertEquals(2, uiState.stats.revealedTickets)
        assertEquals(1, uiState.stats.winningTickets)
        assertEquals("五等奖", uiState.stats.bestPrizeTier)
    }

    private class FakeLotteryRepository(
        initialTickets: MutableList<LotteryTicket> = mutableListOf(),
    ) : LotteryRepository(NoopLotteryApi) {
        private val tickets = initialTickets
        val createdRequests = mutableListOf<LotteryCreateRequest>()
        val checkedIds = mutableListOf<Int>()
        var refreshCalls = 0

        override fun getLotteryHistory(): Flow<ApiResult<List<LotteryTicket>>> {
            refreshCalls += 1
            return flowOf(ApiResult.Loading, ApiResult.Success(tickets.toList()))
        }

        override fun createLottery(request: LotteryCreateRequest): Flow<ApiResult<LotteryTicket>> {
            createdRequests.add(request)
            val ticket = sampleTicket(
                id = (tickets.maxOfOrNull { it.id } ?: 0) + 1,
                issueId = request.issueId,
                revealTime = Instant.parse(request.revealTime),
            )
            tickets.add(ticket)
            return flowOf(ApiResult.Loading, ApiResult.Success(ticket))
        }

        override fun checkLottery(id: Int): Flow<ApiResult<LotteryCheckOutcome>> {
            checkedIds.add(id)
            val result = LotteryResult(
                id = 10,
                type = LOTTERY_TYPE_DA_LE_TOU,
                issueId = "21126",
                numbers = LotteryNumbers(listOf(1, 11, 12, 34, 35), listOf(9, 12)),
                openedAt = Instant.parse("2021-11-03T12:30:00Z"),
                source = "mxnzp",
            )
            val checkedTicket = tickets.first { it.id == id }.copy(
                checked = true,
                resultId = result.id,
                prizeTier = "一等奖",
                frontMatchCount = 5,
                backMatchCount = 2,
                result = result,
            )
            tickets.replaceAll { if (it.id == id) checkedTicket else it }
            return flowOf(
                ApiResult.Loading,
                ApiResult.Success(
                    LotteryCheckOutcome(
                        lottery = checkedTicket,
                        result = result,
                        frontMatchCount = 5,
                        backMatchCount = 2,
                        prizeTier = "一等奖",
                    )
                )
            )
        }
    }

    private object NoopLotteryApi : LotteryApi {
        override suspend fun getLotteryHistory(): List<LotteryHistoryDto> = emptyList()
        override suspend fun createLottery(payload: LotteryCreateRequest): LotteryHistoryDto = error("not used")
        override suspend fun getLottery(lotteryId: Int): LotteryHistoryDto = error("not used")
        override suspend fun updateLottery(lotteryId: Int, payload: LotteryUpdateRequest): LotteryHistoryDto =
            error("not used")

        override suspend fun deleteLottery(lotteryId: Int) = Unit
        override suspend fun checkLottery(lotteryId: Int): LotteryCheckResponseDto = error("not used")
        override suspend fun getLotteryResult(issueId: String): LotteryResultDto = error("not used")
    }
}

private fun sampleTicket(
    id: Int,
    issueId: String = "21126",
    revealTime: Instant = Instant.parse("2021-11-03T12:30:00Z"),
    checked: Boolean = false,
    prizeTier: String? = null,
    frontMatches: Int? = if (checked) 5 else null,
    backMatches: Int? = if (checked) 2 else null,
): LotteryTicket =
    LotteryTicket(
        id = id,
        type = LOTTERY_TYPE_DA_LE_TOU,
        issueId = issueId,
        numbers = LotteryNumbers(listOf(1, 11, 12, 34, 35), listOf(9, 12)),
        revealTime = revealTime,
        capturedImageUri = null,
        checked = checked,
        checkedAt = if (checked) Instant.parse("2026-06-04T10:00:00Z") else null,
        resultId = if (checked) 10 else null,
        prizeTier = prizeTier,
        frontMatchCount = frontMatches,
        backMatchCount = backMatches,
        result = null,
    )
