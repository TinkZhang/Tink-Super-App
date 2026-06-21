package app.tinks.tink.diary

import app.tinks.tink.time.TimeUpsertRequest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

private val DIARY_TIME_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
private val MONDAY_FIRST_WEEK_FIELDS: WeekFields = WeekFields.of(DayOfWeek.MONDAY, 1)

data class Diary(
    val startDate: LocalDate = defaultDiaryDate(),
    val endDate: LocalDate = defaultDiaryDate(),
    val type: DiaryType = DiaryType.Day,
    val title: String = "",
    val content: String = "",
    val timeEntryId: Long? = null,
) {
    val id: String
        get() = "${type.name}: $startDate - $endDate"

    val wordCount: Int
        get() = content.split(Regex("\\s+")).count { it.isNotBlank() }
}

enum class DiaryType(val label: String) {
    Day("Day"),
    Week("Week"),
    Month("Month"),
    Year("Year");

    companion object {
        fun between(start: LocalDate, end: LocalDate): DiaryType {
            val diff = ChronoUnit.DAYS.between(start, end)
            return when {
                diff > 40 -> Year
                diff > 8 -> Month
                diff > 0 -> Week
                else -> Day
            }
        }
    }
}

data class WeeklyRecordData(
    val hasWeekSummary: Boolean?,
    val records: List<DailyRecord>,
)

data class DailyRecord(
    val date: LocalDate,
    val status: RecordStatus,
)

enum class RecordStatus {
    DonePast,
    MissedPast,
    DoneToday,
    TodoToday,
    Future,
}

fun defaultDiaryDate(now: ZonedDateTime = ZonedDateTime.now(DIARY_TIME_ZONE)): LocalDate =
    if (now.hour < 2) now.minusDays(1).toLocalDate() else now.toLocalDate()

fun Diary.displayTime(locale: Locale = Locale.getDefault()): String =
    when (type) {
        DiaryType.Day -> startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
        DiaryType.Week -> {
            val weekNumber = startDate.get(MONDAY_FIRST_WEEK_FIELDS.weekOfYear())
            "${startDate.shortDate(locale)} - ${endDate.shortDate(locale)} - Week $weekNumber"
        }
        DiaryType.Month -> "${startDate.month.getDisplayName(TextStyle.FULL, locale)} ${startDate.year}"
        DiaryType.Year -> startDate.year.toString()
    }

fun Diary.toTimeRequest(): TimeUpsertRequest {
    val start = startDate.atStartOfDay(DIARY_TIME_ZONE).toOffsetDateTime()
    val end = endDate.plusDays(1).atStartOfDay(DIARY_TIME_ZONE).toOffsetDateTime()
    return TimeUpsertRequest(
        type = 1,
        start = start.toString(),
        end = end.toString(),
        title = title.ifBlank { displayTime(Locale.US) },
        description = content.ifBlank { null },
        allDay = true,
        includeInStatistics = false,
        source = "diaryloom",
    )
}

fun Diary.contributionDatesForYear(year: Int): Set<LocalDate> =
    if (type == DiaryType.Day && startDate.year == year) setOf(startDate) else emptySet()

fun weekStart(offset: Int = 0, today: LocalDate = defaultDiaryDate()): LocalDate =
    today
        .plusWeeks(offset.toLong())
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun weekEnd(offset: Int = 0, today: LocalDate = defaultDiaryDate()): LocalDate =
    weekStart(offset, today).plusDays(6)

fun weekRangeLabel(
    offset: Int = 0,
    locale: Locale = Locale.getDefault(),
    today: LocalDate = defaultDiaryDate(),
): String {
    val start = weekStart(offset, today)
    val end = start.plusDays(6)
    val formatter = DateTimeFormatter.ofPattern("MMM d", locale)
    return when {
        start.year != end.year -> "${start.format(formatter)}, ${start.year} - ${end.format(formatter)}, ${end.year}"
        start.month == end.month -> "${start.format(formatter)} - ${end.dayOfMonth}, ${end.year}"
        else -> "${start.format(formatter)} - ${end.format(formatter)}, ${end.year}"
    }
}

fun dateRangeForType(type: DiaryType, anchor: LocalDate = defaultDiaryDate()): Pair<LocalDate, LocalDate> =
    when (type) {
        DiaryType.Day -> anchor to anchor
        DiaryType.Week -> {
            val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            start to start.plusDays(6)
        }
        DiaryType.Month -> anchor.with(TemporalAdjusters.firstDayOfMonth()) to
            anchor.with(TemporalAdjusters.lastDayOfMonth())
        DiaryType.Year -> anchor.with(TemporalAdjusters.firstDayOfYear()) to
            anchor.with(TemporalAdjusters.lastDayOfYear())
    }

private fun LocalDate.shortDate(locale: Locale): String =
    format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale))

val diarySampleDay = Diary(
    startDate = LocalDate.of(2026, 6, 18),
    endDate = LocalDate.of(2026, 6, 18),
    title = "A quiet useful day",
    content = "Shipped a small thing, took a long walk, and wrote before sleep.",
)

val diarySampleWeek = Diary(
    startDate = LocalDate.of(2026, 6, 15),
    endDate = LocalDate.of(2026, 6, 21),
    type = DiaryType.Week,
    title = "Week in review",
    content = "The week had more texture than expected.",
)
