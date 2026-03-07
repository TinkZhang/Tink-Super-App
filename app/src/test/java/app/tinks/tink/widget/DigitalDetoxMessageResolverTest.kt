package app.tinks.tink.widget

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DigitalDetoxMessageResolverTest {

    @Test
    fun resolve_returnsWakeMessage_beforeSeven() {
        val now = LocalDateTime.parse("2026-03-02T06:59:00")

        val message = DigitalDetoxMessageResolver.resolve(now)

        assertEquals(DigitalDetoxMessageResolver.WAKE_UP, message)
    }

    @Test
    fun resolve_returnsWorkFocus_inWorkBlock() {
        val now = LocalDateTime.parse("2026-03-03T10:00:00")

        val message = DigitalDetoxMessageResolver.resolve(now)

        assertEquals(DigitalDetoxMessageResolver.WORK_FOCUS, message)
    }

    @Test
    fun resolve_returnsGoodMorning_betweenSevenAndNine() {
        val now = LocalDateTime.parse("2026-03-03T08:15:00")

        val message = DigitalDetoxMessageResolver.resolve(now)

        assertEquals(DigitalDetoxMessageResolver.GOOD_MORNING, message)
    }

    @Test
    fun resolve_returnsLaunchAndLearn_betweenLunchBreak() {
        val now = LocalDateTime.parse("2026-03-03T12:00:00")

        val message = DigitalDetoxMessageResolver.resolve(now)

        assertEquals(DigitalDetoxMessageResolver.LAUNCH_AND_LEARN, message)
    }

    @Test
    fun resolve_returnsLogTime_onWeekdayLateAfternoon() {
        val now = LocalDateTime.parse("2026-03-03T17:30:00")

        val message = DigitalDetoxMessageResolver.resolve(now)

        assertEquals(DigitalDetoxMessageResolver.LOG_TIME, message)
    }

    @Test
    fun resolve_returnsMindEmpty_onWeekdayEvening() {
        val now = LocalDateTime.parse("2026-03-03T18:30:00")

        val message = DigitalDetoxMessageResolver.resolve(now)

        assertEquals(DigitalDetoxMessageResolver.MIND_EMPTY, message)
    }

    @Test
    fun resolve_returnsTimeForTink_onWeekdayNight() {
        val now = LocalDateTime.parse("2026-03-03T20:00:00")

        val message = DigitalDetoxMessageResolver.resolve(now)

        assertEquals(DigitalDetoxMessageResolver.TIME_FOR_TINK, message)
    }

    @Test
    fun resolve_returnsBedRoom_afterTwentyOne() {
        val now = LocalDateTime.parse("2026-03-03T22:00:00")

        val message = DigitalDetoxMessageResolver.resolve(now)

        assertEquals(DigitalDetoxMessageResolver.BEDROOM, message)
    }

    @Test
    fun resolve_returnsDetox_onWeekendMorning() {
        val now = LocalDateTime.parse("2026-03-07T10:30:00")

        val message = DigitalDetoxMessageResolver.resolve(now)

        assertEquals(DigitalDetoxMessageResolver.DETOX, message)
    }

    @Test
    fun resolve_returnsCountdown_onWeekendAfternoon() {
        val now = LocalDateTime.parse("2026-03-07T15:00:00")

        val message = DigitalDetoxMessageResolver.resolve(now)

        assertEquals(DigitalDetoxMessageResolver.COUNTDOWN, message)
    }
}
