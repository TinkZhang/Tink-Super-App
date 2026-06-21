package app.tinks.tink.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DiaryTimeRequestTest {

    @Test
    fun toTimeRequest_createsAllDayDiaryLoomEntryExcludedFromStatistics() {
        val diary = Diary(
            startDate = LocalDate.of(2026, 6, 15),
            endDate = LocalDate.of(2026, 6, 21),
            type = DiaryType.Week,
            title = "Week in review",
            content = "A week worth remembering.",
        )

        val payload = diary.toTimeRequest()

        assertEquals(1, payload.type)
        assertEquals("2026-06-15T00:00+08:00", payload.start)
        assertEquals("2026-06-22T00:00+08:00", payload.end)
        assertEquals("Week in review", payload.title)
        assertEquals("A week worth remembering.", payload.description)
        assertTrue(payload.allDay)
        assertFalse(payload.includeInStatistics)
        assertEquals("diaryloom", payload.source)
    }
}
