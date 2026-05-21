package app.tinks.tink.weight.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tinks.tink.weight.TrendChartCardUiState
import app.tinks.tink.weight.WeightEvent
import app.tinks.tink.weight.data.Weight
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrendChartCard(
    trendChartCardUiState: TrendChartCardUiState,
    onEvent: (WeightEvent) -> Unit = {},
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weight_trend_card"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "体重趋势",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        if (trendChartCardUiState.selectedIndex == 0) "本月变化" else "全部记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TrendRangeToggle(
                    selectedIndex = trendChartCardUiState.selectedIndex,
                    onSelected = { onEvent(WeightEvent.ChangeSelectedTrendIndex(it)) },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp)
                    .testTag("weight_trend_chart")
            ) {
                if (trendChartCardUiState.weightList.isEmpty()) {
                    Text(
                        "暂无趋势数据",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    WeightTrendCanvas(
                        weights = trendChartCardUiState.weightList,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrendRangeToggle(selectedIndex: Int, onSelected: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val options = listOf("本月", "全部")
        val uncheckedIcons = listOf(Icons.Outlined.CalendarMonth, Icons.Outlined.AllInclusive)
        val checkedIcons = listOf(Icons.Filled.CalendarMonth, Icons.Filled.AllInclusive)

        options.forEachIndexed { index, label ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = { onSelected(index) },
                modifier = Modifier
                    .testTag("weight_trend_${if (index == 0) "month" else "all"}")
                    .semantics { role = Role.RadioButton },
            ) {
                Icon(
                    imageVector = if (selectedIndex == index) checkedIcons[index] else uncheckedIcons[index],
                    contentDescription = label,
                )
                Spacer(Modifier.width(6.dp))
                Text(label)
            }
        }
    }
}

@Composable
private fun WeightTrendCanvas(weights: List<Weight>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val sortedWeights = weights.sortedBy { it.createdTime }

    Canvas(modifier = modifier.padding(top = 8.dp, end = 8.dp, bottom = 6.dp)) {
        val leftPadding = 42.dp.toPx()
        val rightPadding = 10.dp.toPx()
        val topPadding = 10.dp.toPx()
        val bottomPadding = 34.dp.toPx()
        val chartLeft = leftPadding
        val chartRight = size.width - rightPadding
        val chartTop = topPadding
        val chartBottom = size.height - bottomPadding
        val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
        val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

        val minTimestamp = sortedWeights.first().createdTime
        val maxTimestamp = sortedWeights.last().createdTime
        val timestampRange = (maxTimestamp - minTimestamp).coerceAtLeast(1L)

        val minWeightValue = sortedWeights.minOf { it.weight }
        val maxWeightValue = sortedWeights.maxOf { it.weight }
        val paddedMinWeight = floor((minWeightValue - 1.0) * 2.0) / 2.0
        val paddedMaxWeight = ceil((maxWeightValue + 1.0) * 2.0) / 2.0
        val weightRange = (paddedMaxWeight - paddedMinWeight).coerceAtLeast(1.0)

        fun pointFor(weight: Weight): Offset {
            val timeProgress = if (sortedWeights.size == 1) {
                0.5f
            } else {
                ((weight.createdTime - minTimestamp).toFloat() / timestampRange.toFloat())
                    .coerceIn(0f, 1f)
            }
            val valueProgress = ((weight.weight - paddedMinWeight) / weightRange).toFloat()
                .coerceIn(0f, 1f)
            return Offset(
                x = chartLeft + chartWidth * timeProgress,
                y = chartBottom - chartHeight * valueProgress,
            )
        }

        val yTicks = buildWeightTicks(paddedMinWeight, paddedMaxWeight)
        yTicks.forEach { tick ->
            val progress = ((tick - paddedMinWeight) / weightRange).toFloat().coerceIn(0f, 1f)
            val y = chartBottom - chartHeight * progress
            drawLine(
                color = outline,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "%.1f".format(tick),
                topLeft = Offset(0f, y - 8.dp.toPx()),
                style = TextStyle(color = labelColor, fontSize = 10.sp),
            )
        }

        val path = Path()
        sortedWeights.forEachIndexed { index, weight ->
            val point = pointFor(weight)
            if (index == 0) {
                path.moveTo(point.x, point.y)
            } else {
                path.lineTo(point.x, point.y)
            }
        }

        drawPath(
            path = path,
            color = primary,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )

        val fillPath = Path().apply {
            addPath(path)
            lineTo(pointFor(sortedWeights.last()).x, chartBottom)
            lineTo(pointFor(sortedWeights.first()).x, chartBottom)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primary.copy(alpha = 0.22f), Color.Transparent),
                startY = chartTop,
                endY = chartBottom,
            ),
        )

        sortedWeights.forEach { weight ->
            val point = pointFor(weight)
            drawCircle(
                color = primary,
                radius = 4.dp.toPx(),
                center = point,
            )
        }

        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
        val startDate = Instant.ofEpochMilli(minTimestamp).atZone(ZoneId.systemDefault())
        val endDate = Instant.ofEpochMilli(maxTimestamp).atZone(ZoneId.systemDefault())
        drawText(
            textMeasurer = textMeasurer,
            text = startDate.format(dateFormatter),
            topLeft = Offset(chartLeft, chartBottom + 10.dp.toPx()),
            style = TextStyle(color = labelColor, fontSize = 10.sp),
        )
        drawText(
            textMeasurer = textMeasurer,
            text = endDate.format(dateFormatter),
            topLeft = Offset(chartRight - 32.dp.toPx(), chartBottom + 10.dp.toPx()),
            style = TextStyle(color = labelColor, fontSize = 10.sp),
        )
    }
}

private fun buildWeightTicks(minWeight: Double, maxWeight: Double): List<Double> {
    val lower = min(minWeight, maxWeight)
    val upper = max(minWeight, maxWeight)
    val step = max(0.5, ceil((upper - lower) / 4.0 * 2.0) / 2.0)
    return List(5) { index -> lower + step * index }
}

@Preview
@Composable
private fun TrendChartCardPreview() {
    TrendChartCard(
        TrendChartCardUiState(
            selectedIndex = 1,
            weightList = listOf(
                Weight(id = 1, weight = 142.0, createdTime = 1767225600000),
                Weight(id = 2, weight = 141.2, createdTime = 1767657600000),
                Weight(id = 3, weight = 139.8, createdTime = 1768521600000),
                Weight(id = 4, weight = 140.4, createdTime = 1770249600000),
            )
        )
    )
}
