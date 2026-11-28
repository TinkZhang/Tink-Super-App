package app.tinks.tink.yt

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Five-field numeric Unix cron in the device time zone; Sunday is 0 or 7. */
class YtCron private constructor(private val fields: List<Set<Int>>, private val dayWildcard: Boolean, private val weekWildcard: Boolean) {
    fun nextAfter(afterMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        var time = Instant.ofEpochMilli(afterMillis).atZone(zone).truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
        val end = time.plusYears(8)
        while (time.isBefore(end)) {
            if (time.monthValue !in fields[3]) { time = time.plusDays(1).toLocalDate().atStartOfDay(zone); continue }
            val monthDay = time.dayOfMonth in fields[2]
            val weekDay = (time.dayOfWeek.value % 7) in fields[4]
            val dayMatches = when {
                dayWildcard || weekWildcard -> monthDay && weekDay
                else -> monthDay || weekDay
            }
            if (!dayMatches) { time = time.plusDays(1).toLocalDate().atStartOfDay(zone); continue }
            if (time.hour in fields[1] && time.minute in fields[0]) return time.toInstant().toEpochMilli()
            time = time.plusMinutes(1)
        }
        throw IllegalArgumentException("Cron has no matching date in the next eight years.")
    }

    companion object {
        fun parse(expression: String): YtCron {
            val parts = expression.trim().split(Regex("\\s+"))
            require(parts.size == 5) { "Use five cron fields: minute hour day month weekday." }
            val ranges = listOf(0..59, 0..23, 1..31, 1..12, 0..7)
            val fields = parts.mapIndexed { index, part ->
                val allowed = ranges[index]
                val values = mutableSetOf<Int>()
                part.split(',').forEach { term ->
                    val stepParts = term.split('/')
                    require(stepParts.size in 1..2) { "Invalid cron step." }
                    val step = if (stepParts.size == 2) stepParts[1].toIntOrNull() else 1
                    require(step != null && step > 0) { "Invalid cron step." }
                    val base = stepParts[0]
                    val bounds = when {
                        base == "*" -> allowed
                        '-' in base -> {
                            val pair = base.split('-')
                            require(pair.size == 2) { "Invalid cron range." }
                            val start = pair[0].toIntOrNull(); val end = pair[1].toIntOrNull()
                            require(start != null && end != null && start <= end) { "Invalid cron range." }
                            start..end
                        }
                        else -> {
                            val start = base.toIntOrNull()
                            require(start != null) { "Cron fields must use numbers, *, ranges, lists or steps." }
                            start..(if (stepParts.size == 2) allowed.last else start)
                        }
                    }
                    require(bounds.first in allowed && bounds.last in allowed) { "Cron field ${index + 1} must be within ${allowed.first}–${allowed.last}." }
                    values.addAll(bounds step step)
                }
                if (index == 4) values.map { it % 7 }.toSet() else values
            }
            return YtCron(fields, parts[2].startsWith("*"), parts[4].startsWith("*"))
        }
    }
}

fun validateSubscriptionOptions(cron: String, latestCount: Int) {
    require(latestCount in 1..500) { "Latest episodes must be between 1 and 500." }
    YtCron.parse(cron).nextAfter(System.currentTimeMillis())
}
