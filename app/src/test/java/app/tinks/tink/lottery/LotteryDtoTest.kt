package app.tinks.tink.lottery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LotteryDtoTest {

    @Test
    fun historyDto_mapsBackendFieldsToDomain() {
        val dto = LotteryHistoryDto(
            id = 7,
            createdAt = "2026-06-04T09:00:00Z",
            type = LOTTERY_TYPE_DA_LE_TOU,
            issueId = "21126",
            frontNumbers = listOf(35, 1, 11, 12, 34),
            backNumbers = listOf(12, 9),
            revealTime = "2021-11-03T12:30:00Z",
            checked = true,
            checkedAt = "2026-06-04T10:00:00Z",
            resultId = 3,
            prizeTier = "一等奖",
            frontMatchCount = 5,
            backMatchCount = 2,
            result = LotteryResultDto(
                id = 3,
                createdAt = "2026-06-04T09:30:00Z",
                type = LOTTERY_TYPE_DA_LE_TOU,
                issueId = "21126",
                frontNumbers = listOf(1, 11, 12, 34, 35),
                backNumbers = listOf(9, 12),
                openedAt = "2021-11-03T12:30:00Z",
                source = "mxnzp",
            ),
        )

        val domain = dto.toDomain()

        assertEquals(7, domain.id)
        assertEquals("21126", domain.issueId)
        assertEquals(listOf(1, 11, 12, 34, 35), domain.numbers.front)
        assertEquals(listOf(9, 12), domain.numbers.back)
        assertEquals(Instant.parse("2021-11-03T12:30:00Z"), domain.revealTime)
        assertTrue(domain.checked)
        assertEquals("一等奖", domain.prizeTier)
        assertEquals("mxnzp", domain.result?.source)
    }

    @Test
    fun draftToCreateRequest_validatesAndNormalizesIssueAndNumbers() {
        val draft = LotteryDraft(
            issueId = "第21126期",
            frontNumbersText = "35 01 11 12 34",
            backNumbersText = "12 09",
            revealTimeText = "2021-11-03T12:30:00Z",
        )

        val request = draft.toCreateRequest()

        assertEquals("21126", request.issueId)
        assertEquals(listOf(1, 11, 12, 34, 35), request.frontNumbers)
        assertEquals(listOf(9, 12), request.backNumbers)
    }
}
