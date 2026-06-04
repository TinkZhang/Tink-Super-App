package app.tinks.tink.lottery

import app.tinks.tink.network.ApiResult
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LotteryRepositoryTest {

    @Test
    fun getLotteryHistory_emitsLoadingThenSortedHistory() = runTest {
        val api = FakeLotteryApi(
            history = mutableListOf(
                sampleHistoryDto(id = 1, issueId = "21125", revealTime = "2021-11-01T12:30:00Z"),
                sampleHistoryDto(id = 2, issueId = "21126", revealTime = "2021-11-03T12:30:00Z"),
            )
        )
        val repository = LotteryRepository(api)

        val emissions = repository.getLotteryHistory().take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        val success = emissions[1] as ApiResult.Success
        assertEquals(listOf(2, 1), success.data.map { it.id })
    }

    @Test
    fun createLottery_postsPayload() = runTest {
        val api = FakeLotteryApi()
        val repository = LotteryRepository(api)
        val request = LotteryCreateRequest(
            issueId = "21126",
            frontNumbers = listOf(1, 11, 12, 34, 35),
            backNumbers = listOf(9, 12),
            revealTime = "2021-11-03T12:30:00Z",
        )

        val emissions = repository.createLottery(request).take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        assertTrue(emissions[1] is ApiResult.Success)
        assertEquals(request, api.createdPayloads.single())
    }

    @Test
    fun checkLottery_callsCheckEndpoint() = runTest {
        val api = FakeLotteryApi()
        val repository = LotteryRepository(api)

        val emissions = repository.checkLottery(42).take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        val success = emissions[1] as ApiResult.Success
        assertEquals(listOf(42), api.checkedIds)
        assertEquals("一等奖", success.data.prizeTier)
    }

    @Test
    fun deleteLottery_callsDeleteEndpoint() = runTest {
        val api = FakeLotteryApi()
        val repository = LotteryRepository(api)

        val emissions = repository.deleteLottery(42).take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        assertTrue(emissions[1] is ApiResult.Success)
        assertEquals(listOf(42), api.deletedIds)
    }

    private class FakeLotteryApi(
        private val history: MutableList<LotteryHistoryDto> = mutableListOf(),
    ) : LotteryApi {
        val createdPayloads = mutableListOf<LotteryCreateRequest>()
        val checkedIds = mutableListOf<Int>()
        val deletedIds = mutableListOf<Int>()

        override suspend fun getLotteryHistory(): List<LotteryHistoryDto> = history

        override suspend fun createLottery(payload: LotteryCreateRequest): LotteryHistoryDto {
            createdPayloads.add(payload)
            return sampleHistoryDto(
                id = 99,
                issueId = payload.issueId,
                revealTime = payload.revealTime,
                frontNumbers = payload.frontNumbers,
                backNumbers = payload.backNumbers,
            )
        }

        override suspend fun getLottery(lotteryId: Int): LotteryHistoryDto =
            history.firstOrNull { it.id == lotteryId } ?: sampleHistoryDto(id = lotteryId)

        override suspend fun updateLottery(
            lotteryId: Int,
            payload: LotteryUpdateRequest,
        ): LotteryHistoryDto = sampleHistoryDto(id = lotteryId)

        override suspend fun deleteLottery(lotteryId: Int) {
            deletedIds.add(lotteryId)
        }

        override suspend fun checkLottery(lotteryId: Int): LotteryCheckResponseDto {
            checkedIds.add(lotteryId)
            val result = sampleResultDto()
            return LotteryCheckResponseDto(
                lottery = sampleHistoryDto(id = lotteryId, checked = true, result = result),
                result = result,
                frontMatchCount = 5,
                backMatchCount = 2,
                prizeTier = "一等奖",
            )
        }

        override suspend fun getLotteryResult(issueId: String): LotteryResultDto =
            sampleResultDto(issueId = issueId)
    }
}

private fun sampleHistoryDto(
    id: Int = 1,
    issueId: String = "21126",
    revealTime: String = "2021-11-03T12:30:00Z",
    checked: Boolean = false,
    result: LotteryResultDto? = null,
    frontNumbers: List<Int> = listOf(1, 11, 12, 34, 35),
    backNumbers: List<Int> = listOf(9, 12),
): LotteryHistoryDto =
    LotteryHistoryDto(
        id = id,
        createdAt = "2026-06-04T09:00:00Z",
        type = LOTTERY_TYPE_DA_LE_TOU,
        issueId = issueId,
        frontNumbers = frontNumbers,
        backNumbers = backNumbers,
        revealTime = revealTime,
        checked = checked,
        checkedAt = if (checked) "2026-06-04T10:00:00Z" else null,
        resultId = result?.id,
        prizeTier = if (checked) "一等奖" else null,
        frontMatchCount = if (checked) 5 else null,
        backMatchCount = if (checked) 2 else null,
        result = result,
    )

private fun sampleResultDto(issueId: String = "21126"): LotteryResultDto =
    LotteryResultDto(
        id = 3,
        createdAt = "2026-06-04T09:00:00Z",
        type = LOTTERY_TYPE_DA_LE_TOU,
        issueId = issueId,
        frontNumbers = listOf(1, 11, 12, 34, 35),
        backNumbers = listOf(9, 12),
        openedAt = "2021-11-03T12:30:00Z",
        source = "mxnzp",
    )
