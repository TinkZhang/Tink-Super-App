package app.tinks.tink.diary

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DiaryStateTest {

    @Test
    fun contributionDates_usesDailyEntriesInContributionYear() {
        val state = DiaryUiState(
            contributionYear = 2026,
            diaries = listOf(
                Diary(
                    startDate = LocalDate.of(2026, 1, 3),
                    endDate = LocalDate.of(2026, 1, 3),
                    type = DiaryType.Day,
                ),
                Diary(
                    startDate = LocalDate.of(2026, 1, 1),
                    endDate = LocalDate.of(2026, 1, 31),
                    type = DiaryType.Month,
                ),
                Diary(
                    startDate = LocalDate.of(2025, 12, 31),
                    endDate = LocalDate.of(2025, 12, 31),
                    type = DiaryType.Day,
                ),
            ),
        )

        assertEquals(setOf(LocalDate.of(2026, 1, 3)), state.contributionDates)
    }

    @Test
    fun dateRangeForType_weekStartsOnMonday() {
        val (start, end) = dateRangeForType(DiaryType.Week, anchor = LocalDate.of(2026, 6, 19))

        assertEquals(LocalDate.of(2026, 6, 15), start)
        assertEquals(LocalDate.of(2026, 6, 21), end)
    }

    @Test
    fun weekStartAndLabel_useMondayFirstWeek() {
        val today = LocalDate.of(2026, 6, 19)

        assertEquals(LocalDate.of(2026, 6, 15), weekStart(today = today))
        assertEquals(LocalDate.of(2026, 6, 22), weekStart(offset = 1, today = today))
        assertEquals("Jun 15 - 21, 2026", weekRangeLabel(locale = Locale.US, today = today))
    }
}
