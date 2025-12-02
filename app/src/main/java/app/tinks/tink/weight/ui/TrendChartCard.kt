package app.tinks.tink.weight.ui

import android.widget.ToggleButton
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonGroupDefaults
import app.tinks.tink.weight.TrendChartCardUiState
import app.tinks.tink.weight.WeightEvent
import app.tinks.tink.weight.data.Weight


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrendChartCard(
    trendChartCardUiState: TrendChartCardUiState,
    onEvent: (WeightEvent) -> Unit = {}
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("体重趋势", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(4.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.Spacing),
                    ) {
                        val modifiers =
                            listOf(Modifier.weight(1f), Modifier.weight(1.5f), Modifier.weight(1f))
                        val options = listOf("月", "年", "所有")
                        val unCheckedIcons =
                            listOf(
                                Icons.Outlined.CalendarMonth,
                                Icons.Outlined.CalendarMonth,
                                Icons.Outlined.AllInclusive
                            )
                        val checkedIcons = listOf(
                            Icons.Filled.CalendarMonth,
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
                                modifier = modifiers[index].semantics { role = Role.RadioButton },
//                                shapes =
//                                    when (index) {
//                                        0 -> ButtonGroupDefaults.
//                                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
//                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
//                                    },
                            ) {
                                Icon(
                                    if (trendChartCardUiState.selectedIndex == index) checkedIcons[index] else unCheckedIcons[index],
                                    contentDescription = "Localized description",
                                )
                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text(label)
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
                    .height(150.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val historyData = trendChartCardUiState.weightList.map { it.weight }
                if (historyData.isEmpty()) {
                    Text("暂无数据", modifier = Modifier.align(Alignment.Center))
                } else {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxVal = historyData.maxOrNull()?.toFloat() ?: 100f
                        val minVal = historyData.minOrNull()?.toFloat() ?: 0f
                        val range = (maxVal - minVal).coerceAtLeast(1f)

                        val widthPerPoint = size.width / (historyData.size - 1).coerceAtLeast(1)

                        val path = Path()

                        historyData.forEachIndexed { index, value ->
                            // 归一化高度 (值越大，y坐标越小)
                            val normalizedY = size.height - ((value - minVal) / range) * size.height
                            val x = index * widthPerPoint

                            if (index == 0) {
                                path.moveTo(x, normalizedY.toFloat())
                            } else {
                                // 简单的直线连接，也可以做贝塞尔曲线
                                path.lineTo(x, normalizedY.toFloat())
                            }

                            // 绘制点
                            drawCircle(
                                color = primaryColor,
                                radius = 4.dp.toPx(),
                                center = Offset(x, normalizedY.toFloat())
                            )
                        }

                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 渐变填充 (可选)
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