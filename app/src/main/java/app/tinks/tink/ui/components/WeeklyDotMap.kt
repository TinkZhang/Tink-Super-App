package app.tinks.tink.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

enum class RecordType {
    DONE_PAST, MISSED_PAST, DONE_TODAY, TODO_TODAY, FUTURE
}

data class DailyRecord(
    val date: LocalDate,
    val status: RecordType
)

data class WeeklyRecordData(
    val hasWeekSummary: Boolean? = null,
    val records: List<DailyRecord>,
) {
    companion object {
        fun fromWeeklyInt(list: List<Int?>) = WeeklyRecordData(records = mapToDailyRecords(list))
    }
}

private fun mapToDailyRecords(values: List<Int?>): List<DailyRecord> {
    require(values.size == 7)

    val today = LocalDate.now()
    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    return values.mapIndexed { index, value ->
        val date = startOfWeek.plusDays(index.toLong())

        val status = when {
            value == null ->
                RecordType.FUTURE

            date.isEqual(today) && value > 0 ->
                RecordType.DONE_TODAY

            date.isEqual(today) && value == 0 ->
                RecordType.TODO_TODAY

            date.isBefore(today) && value > 0 ->
                RecordType.DONE_PAST

            date.isBefore(today) && value == 0 ->
                RecordType.MISSED_PAST

            else ->
                RecordType.FUTURE
        }

        DailyRecord(date, status)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WeeklyRecordMap(
    data: WeeklyRecordData,
    modifier: Modifier = Modifier,
    swipeOnMap: (Int) -> Unit = {},
    locale: Locale = Locale.getDefault()
) {
    var weekOffset by remember { mutableIntStateOf(0) }

    val swipeThreshold = 100f
    val dragAmount = remember { mutableFloatStateOf(0f) }
    val transitionDirection = remember { mutableIntStateOf(0) }

    val currentWeekRange = remember(weekOffset, locale) {
        getWeekRangeLabel(weekOffset = weekOffset, locale = locale)
    }
    LaunchedEffect(true) { swipeOnMap(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            dragAmount.floatValue > swipeThreshold -> {
                                transitionDirection.intValue = 1
                                weekOffset -= 1
                            }

                            dragAmount.floatValue < -swipeThreshold -> {
                                transitionDirection.intValue = -1
                                weekOffset += 1
                            }
                        }
                        dragAmount.floatValue = 0f
                        swipeOnMap(weekOffset)
                    },
                    onHorizontalDrag = { _, dragDistance ->
                        dragAmount.floatValue += dragDistance
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = currentWeekRange,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 8.dp)
            )

            AnimatedContent(
                targetState = weekOffset,
                transitionSpec = {
                    if (transitionDirection.intValue == -1) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }.using(SizeTransform(clip = false))
                }
            ) { offset ->
                WeeklyRecordMap(data = data, offset = offset)
            }
        }
    }
}

@Composable
fun WeeklyRecordMap(
    data: WeeklyRecordData,
    offset: Int,
    modifier: Modifier = Modifier,
    locale: Locale = Locale.getDefault()
) {

    val dayLabels = remember(locale) { getLocalizedWeekdayLabels(locale) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                other = when (data.hasWeekSummary) {
                    true -> Modifier.border(
                        width = 4.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    false -> Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(8.dp),
                    )

                    null -> Modifier
                }
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        data.records.forEachIndexed { index, record ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (record.status) {
                    RecordType.DONE_PAST -> DoneDot()
                    RecordType.MISSED_PAST -> MissedCross()
                    RecordType.DONE_TODAY -> DoneToday()
                    RecordType.TODO_TODAY -> TodoToday()
                    RecordType.FUTURE -> TodoCircle()
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dayLabels[index],
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DoneToday(size: Dp = 32.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                color = MaterialTheme.colorScheme.primary, shape = MaterialShapes.Sunny.toShape()
            )
    )
}

@Composable
fun MissedCross(size: Dp = 24.dp) {
    val color = MaterialTheme.colorScheme.error
    Box(
        modifier = Modifier.size(size), contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.toPx() * 0.1f
            val length = size.toPx()
            drawLine(color, Offset(0f, 0f), Offset(length, length), strokeWidth)
            drawLine(color, Offset(length, 0f), Offset(0f, length), strokeWidth)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TodoToday(size: Dp = 32.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.tertiary,
                shape = MaterialShapes.Sunny.toShape()
            )
    )
}

@Composable
fun TodoCircle(size: Dp = 24.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .border(
                width = 2.dp, color = MaterialTheme.colorScheme.tertiary, shape = CircleShape
            )
    )
}

@Composable
fun DoneDot(size: Dp = 24.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
    )
}

fun getWeekRangeLabel(
    date: LocalDate = LocalDate.now(),
    weekOffset: Int = 0,
    locale: Locale = Locale.getDefault(),
): String {
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek

    // Apply week offset if provided
    val targetDate = date.plusWeeks(weekOffset.toLong())

    // Get start and end of the week
    val weekStart = targetDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val weekEnd = weekStart.plusDays(6)

    val formatter = DateTimeFormatter.ofPattern("MMM d", locale)

    return when {
        weekStart.year != weekEnd.year -> {
            // Different years → show year for both
            "${weekStart.format(formatter)}, ${weekStart.year} – ${weekEnd.format(formatter)}, ${weekEnd.year}"
        }

        weekStart.month == weekEnd.month -> {
            // Same month, same year → avoid repeating month name
            "${weekStart.format(formatter)} – ${weekEnd.dayOfMonth}, ${weekEnd.year}"
        }

        else -> {
            // Same year but different months
            "${weekStart.format(formatter)} – ${weekEnd.format(formatter)}, ${weekEnd.year}"
        }
    }
}

fun getLocalizedWeekdayLabels(locale: Locale = Locale.getDefault()): List<String> {
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val days = DayOfWeek.entries

    val startIndex = days.indexOf(firstDay)
    val orderedDays = (days.subList(startIndex, days.size) + days.subList(0, startIndex))

    return orderedDays.map { dayOfWeek ->
        dayOfWeek.getDisplayName(TextStyle.SHORT, locale) // e.g., "Mon", "Tue"
    }
}