package app.tinks.tink.yt

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class YtCronTest {
    private fun next(cron: String, after: String, zone: String = "UTC") =
        Instant.ofEpochMilli(YtCron.parse(cron).nextAfter(Instant.parse(after).toEpochMilli(), ZoneId.of(zone))).toString()

    @Test fun dailyUsesLocalTimeAndNextOccurrenceIsStrictlyAfterNow() {
        assertEquals("2026-09-16T00:00:00Z", next("0 8 * * *", "2026-09-15T00:00:00Z", "Asia/Shanghai"))
        assertEquals("2026-09-15T00:00:00Z", next("0 8 * * *", "2026-09-14T23:59:59Z", "Asia/Shanghai"))
    }
    @Test fun supportsRangesListsStepsAndSundayAliases() {
        assertEquals("2026-09-15T06:00:00Z", next("0 */6 * * *", "2026-09-15T01:00:00Z"))
        assertEquals("2026-09-15T09:15:00Z", next("15,45 9-17 * * 1-5", "2026-09-15T09:14:59Z"))
        assertEquals(next("0 8 * * 0", "2026-09-15T00:00:00Z"), next("0 8 * * 7", "2026-09-15T00:00:00Z"))
    }
    @Test fun usesUnixDayOrSemanticsAndWildcardSteps() {
        assertEquals("2026-09-18T08:00:00Z", next("0 8 1 * 5", "2026-09-15T00:00:00Z"))
        assertEquals("2026-09-17T08:00:00Z", next("0 8 */2 * *", "2026-09-15T08:00:00Z"))
    }
    @Test fun handlesLeapYearsAndMissingDstHour() {
        assertEquals("2028-02-29T00:00:00Z", next("0 0 29 2 *", "2026-01-01T00:00:00Z"))
        assertEquals("2026-03-09T06:30:00Z", next("30 2 * * *", "2026-03-08T06:59:00Z", "America/New_York"))
    }
    @Test fun rejectsInvalidAndImpossibleSchedulesAndEpisodeCounts() {
        listOf("", "* * *", "60 * * * *", "*/0 * * * *", "0 0 32 * *", "0 0 * * 8", "0 0 * 4-2 *", "@daily").forEach {
            assertThrows(it, IllegalArgumentException::class.java) { YtCron.parse(it) }
        }
        assertThrows(IllegalArgumentException::class.java) { next("0 0 30 2 *", "2026-01-01T00:00:00Z") }
        assertThrows(IllegalArgumentException::class.java) { validateSubscriptionOptions("0 8 * * *", 0) }
        assertThrows(IllegalArgumentException::class.java) { validateSubscriptionOptions("0 8 * * *", 501) }
    }
}
