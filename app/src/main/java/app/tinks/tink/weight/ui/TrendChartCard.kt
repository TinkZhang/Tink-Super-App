package app.tinks.tink.weight.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tinks.tink.ui.components.ContentCard
import app.tinks.tink.weight.TrendChartCardUiState
import app.tinks.tink.weight.WeightEvent
import app.tinks.tink.weight.data.Weight
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrendChartCard(
    trendChartCardUiState: TrendChartCardUiState,
    onEvent: (WeightEvent) -> Unit = {}
) {
    ContentCard(title = "体重趋势", modifier = Modifier.fillMaxWidth()) {
        Column() {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(4.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val modifiers =
                            listOf(Modifier.weight(1f), Modifier.weight(1.5f))
                        val options = listOf("月", "所有")
                        val unCheckedIcons =
                            listOf(
                                Icons.Outlined.CalendarMonth,
                                Icons.Outlined.AllInclusive
                            )
                        val checkedIcons = listOf(
                            Icons.Filled.CalendarMonth,
                            Icons.Filled.AllInclusive
                        )
                        options.forEachIndexed { index, label ->
                            ToggleButton(
                                checked = trendChartCardUiState.selectedIndex == index,
                                onCheckedChange = {
                                    onEvent(
                                        WeightEvent.ChangeSelectedTrendIndex(
                                            index
                                        )
                                    )
                                },
                                modifier = (if (trendChartCardUiState.selectedIndex == index) modifiers[1] else modifiers[0]).semantics {
                                    role = Role.RadioButton
                                },
                            ) {
                                Icon(
                                    if (trendChartCardUiState.selectedIndex == index) checkedIcons[index] else unCheckedIcons[index],
                                    contentDescription = label,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val weightList = trendChartCardUiState.weightList
                if (weightList.isEmpty()) {
                    Text("暂无数据", modifier = Modifier.align(Alignment.Center))
                } else {
                    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
                    val textMeasurer = rememberTextMeasurer()
                    val primaryColor = MaterialTheme.colorScheme.primary
                    
                    // Y-axis labels (outside canvas, positioned absolutely)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 8.dp, end = 8.dp)
                    ) {
                        // Calculate Y positions for labels
                        val chartHeight = 200.dp
                        val minWeight = 130f
                        val maxWeight = 150f
                        val range = maxWeight - minWeight
                        
                        val weights = listOf(130f, 135f, 140f, 145f, 150f)
                        weights.forEach { weight ->
                            val normalizedY = 1f - ((weight - minWeight) / range)
                            val yOffset = (chartHeight.value * normalizedY).dp
                            
                            Text(
                                text = weight.toInt().toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(y = yOffset - 6.dp)
                            )
                        }
                    }
                    
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 32.dp, end = 8.dp) // Add left padding for Y-axis labels
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    // Detect which point was tapped
                                    val chartHeight = size.height
                                    val chartWidth = size.width
                                    val minWeight = 135f
                                    val maxWeight = 145f
                                    val range = maxWeight - minWeight
                                    val widthPerPoint = chartWidth / (weightList.size - 1).coerceAtLeast(1)
                                    
                                    selectedPointIndex = weightList.indices.minByOrNull { index ->
                                        val weight = weightList[index].weight.toFloat()
                                        val normalizedY = chartHeight - ((weight - minWeight) / range) * chartHeight
                                        val x = index * widthPerPoint
                                        val distance = kotlin.math.sqrt(
                                            (offset.x - x) * (offset.x - x) +
                                            (offset.y - normalizedY) * (offset.y - normalizedY)
                                        )
                                        distance
                                    }?.takeIf { index ->
                                        val weight = weightList[index].weight.toFloat()
                                        val normalizedY = chartHeight - ((weight - minWeight) / range) * chartHeight
                                        val x = index * widthPerPoint
                                        val distance = kotlin.math.sqrt(
                                            (offset.x - x) * (offset.x - x) +
                                            (offset.y - normalizedY) * (offset.y - normalizedY)
                                        )
                                        distance < 30f // Touch tolerance
                                    }
                                }
                            }
                    ) {
                        val minWeight = 130f  // Expanded range to show 130-150
                        val maxWeight = 150f
                        val range = maxWeight - minWeight
                        val widthPerPoint = size.width / (weightList.size - 1).coerceAtLeast(1)
                        
                        // Draw horizontal reference lines
                        val y130 = size.height - ((130f - minWeight) / range) * size.height
                        val y135 = size.height - ((135f - minWeight) / range) * size.height
                        val y140 = size.height - ((140f - minWeight) / range) * size.height
                        val y145 = size.height - ((145f - minWeight) / range) * size.height
                        val y150 = size.height - ((150f - minWeight) / range) * size.height
                        
                        // Draw 130kg line (primary color, thin)
                        drawLine(
                            color = primaryColor.copy(alpha = 0.3f),
                            start = Offset(0f, y130),
                            end = Offset(size.width, y130),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                        
                        // Draw 135kg line (green)
                        drawLine(
                            color = Color.Green.copy(alpha = 0.5f),
                            start = Offset(0f, y135),
                            end = Offset(size.width, y135),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                        
                        // Draw 140kg line (primary color, thin)
                        drawLine(
                            color = primaryColor.copy(alpha = 0.3f),
                            start = Offset(0f, y140),
                            end = Offset(size.width, y140),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                        
                        // Draw 145kg line (red)
                        drawLine(
                            color = Color.Red.copy(alpha = 0.5f),
                            start = Offset(0f, y145),
                            end = Offset(size.width, y145),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                        
                        // Draw 150kg line (primary color, thin)
                        drawLine(
                            color = primaryColor.copy(alpha = 0.3f),
                            start = Offset(0f, y150),
                            end = Offset(size.width, y150),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                        
                        // Draw weight labels for reference lines (positioned to the left of chart) - REMOVED
                        // Labels are now drawn outside Canvas using Text composables
                        
                        // Draw path connecting points
                        val path = Path()
                        weightList.forEachIndexed { index, weight ->
                            val value = weight.weight.toFloat()
                            val normalizedY = size.height - ((value - minWeight) / range) * size.height
                            val x = index * widthPerPoint
                            
                            if (index == 0) {
                                path.moveTo(x, normalizedY)
                            } else {
                                path.lineTo(x, normalizedY)
                            }
                        }
                        
                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                        
                        // Gradient fill shadow effect
                        val fillPath = Path()
                        fillPath.addPath(path)
                        fillPath.lineTo(size.width, size.height)
                        fillPath.lineTo(0f, size.height)
                        fillPath.close()
                        
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                        
                        // Draw data points with color based on weight
                        weightList.forEachIndexed { index, weight ->
                            val value = weight.weight.toFloat()
                            val normalizedY = size.height - ((value - minWeight) / range) * size.height
                            val x = index * widthPerPoint
                            
                            // Determine point color
                            val pointColor = when {
                                value < 135f -> Color.Green
                                value > 145f -> Color.Red
                                else -> Color(0xFFFFEB3B) // Yellow
                            }
                            
                            // Draw larger circle if selected
                            val radius = if (selectedPointIndex == index) 6.dp.toPx() else 4.dp.toPx()
                            
                            drawCircle(
                                color = pointColor,
                                radius = radius,
                                center = Offset(x, normalizedY)
                            )
                        }
                        
                        // Draw X-axis labels (dates) for current month view
                        if (trendChartCardUiState.selectedIndex == 0 && weightList.isNotEmpty()) {
                            weightList.forEachIndexed { index, weight ->
                                val date = Instant.ofEpochMilli(weight.createdTime)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                val day = date.dayOfMonth
                                val x = index * widthPerPoint
                                
                                // Draw day number below chart
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = day.toString(),
                                    topLeft = Offset(x - 8f, size.height + 4f),
                                    style = TextStyle(
                                        color = Color.Gray,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    }
                    
                    // Tooltip for selected point
                    selectedPointIndex?.let { index ->
                        val weight = weightList[index]
                        val date = Instant.ofEpochMilli(weight.createdTime)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MM-dd"))
                        
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-8).dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${weight.weight} kg",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun TrendChartCardPreview() {
    TrendChartCard(
        TrendChartCardUiState(
            selectedIndex = 1,
            weightList = listOf(
                Weight(id = 1, weight = 100.0, createdTime = 12312131),
                Weight(id = 2, weight = 110.0, createdTime = 12312131),
                Weight(id = 3, weight = 105.0, createdTime = 12312131),
                Weight(id = 4, weight = 106.0, createdTime = 12312131),
            )
        )
    )
}