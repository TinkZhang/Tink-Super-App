package app.tinks.tink.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit

@Composable
fun YearContributionGraph(
    year: Int,
    markedDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
    markedColor: Color = MaterialTheme.colorScheme.primary,
    emptyColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
) {
    val days = remember(year) {
        val firstDay = LocalDate.of(year, 1, 1)
        val lastDay = LocalDate.of(year, 12, 31)
        val gridStart = firstDay.minusDays((firstDay.dayOfWeek.value % 7).toLong())
        val weekCount = (ChronoUnit.WEEKS.between(gridStart, lastDay) + 1).toInt()
        (0 until weekCount).flatMap { week ->
            (0..6).map { day ->
                gridStart.plusDays((week * 7 + day).toLong())
            }
        }
    }
    val weekCount = remember(days) { (days.size / 7).coerceAtLeast(1) }
    val yearValue = remember(year) { Year.of(year) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .testTag("year_contribution_graph"),
    ) {
        val gap = 2.dp.toPx()
        val cellWidth = (size.width - gap * (weekCount - 1)) / weekCount
        val cellHeight = (size.height - gap * 6) / 7
        val cell = minOf(cellWidth, cellHeight)
        val corner = CornerRadius(2.dp.toPx(), 2.dp.toPx())

        days.forEachIndexed { index, date ->
            if (Year.from(date) != yearValue) return@forEachIndexed

            val week = index / 7
            val day = index % 7
            val x = week * (cell + gap)
            val y = day * (cell + gap)
            drawRoundRect(
                color = if (date in markedDates) markedColor else emptyColor,
                topLeft = Offset(x, y),
                size = Size(cell, cell),
                cornerRadius = corner,
            )
        }
    }
}
