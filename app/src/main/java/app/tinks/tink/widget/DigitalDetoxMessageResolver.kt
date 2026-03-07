package app.tinks.tink.widget

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

internal object DigitalDetoxMessageResolver {
    const val WAKE_UP = "No Phone on WakeUp."
    const val GOOD_MORNING = "Good Morning"
    const val LAUNCH_AND_LEARN = "Launch & Learn"
    const val WORK_FOCUS = "Keep Phone OFF and Keep your WORK DONE"
    const val LOG_TIME = "Log your TIME before leaving office"
    const val MIND_EMPTY = "Keep your mind EMPTY"
    const val TIME_FOR_TINK = "Time for Tink"
    const val BEDROOM = "No Phone in Bedroom"
    const val DETOX = "Digital Detox"
    const val COUNTDOWN = "3..2..1.."

    fun resolve(now: LocalDateTime = LocalDateTime.now()): String {
        val time = now.toLocalTime()
        if (time.inRange(start = 0, end = 7 * 60)) return WAKE_UP

        return if (now.dayOfWeek.isWeekend()) {
            resolveWeekend(time)
        } else {
            resolveWeekday(time)
        }
    }

    private fun resolveWeekday(time: LocalTime): String = when {
        time.inRange(start = 7 * 60, end = 9 * 60) -> GOOD_MORNING
        time.inRange(start = 11 * 60 + 30, end = 13 * 60 + 30) -> LAUNCH_AND_LEARN
        time.inRange(start = 9 * 60, end = 11 * 60 + 30) ||
            time.inRange(start = 13 * 60 + 30, end = 17 * 60) -> WORK_FOCUS
        time.inRange(start = 17 * 60, end = 18 * 60) -> LOG_TIME
        time.inRange(start = 18 * 60, end = 19 * 60 + 30) -> MIND_EMPTY
        time.inRange(start = 19 * 60 + 30, end = 21 * 60) -> TIME_FOR_TINK
        time.inRange(start = 21 * 60, end = 24 * 60) -> BEDROOM
        else -> WORK_FOCUS
    }

    private fun resolveWeekend(time: LocalTime): String = when {
        time.inRange(start = 7 * 60, end = 14 * 60) -> DETOX
        time.inRange(start = 14 * 60, end = 21 * 60) -> COUNTDOWN
        else -> BEDROOM
    }

    private fun DayOfWeek.isWeekend(): Boolean = this == DayOfWeek.SATURDAY || this == DayOfWeek.SUNDAY

    private fun LocalTime.inRange(start: Int, end: Int): Boolean {
        val minuteOfDay = hour * 60 + minute
        return minuteOfDay in start until end
    }
}
