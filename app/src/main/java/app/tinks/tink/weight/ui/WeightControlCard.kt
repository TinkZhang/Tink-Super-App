package app.tinks.tink.weight.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tinks.tink.weight.WeightControlCardUiState
import app.tinks.tink.weight.WeightEvent
import app.tinks.tink.weight.data.Weight

@Composable
fun WeightControlCard(
    weightControlCardUiState: WeightControlCardUiState,
    onEvent: (WeightEvent) -> Unit = {},
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weight_control_card"), colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部状态 Chip
            if (!weightControlCardUiState.isTodayRecorded) {
                AssistChip(
                    onClick = {},
                    label = { Text("上次记录: ${weightControlCardUiState.lastDateText}") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.error
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        enabled = true, borderColor = MaterialTheme.colorScheme.error
                    )
                )
            } else {
                AssistChip(onClick = {}, label = { Text("今日已记录") }, leadingIcon = {
                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                })
            }

            Spacer(Modifier.height(24.dp))

            // 体重数值显示与滑动交互区
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("weight_drag_area")
                    // 垂直拖动逻辑
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            // 灵敏度调节: dragAmount / 50 意味着每滑动50px改变0.1
                            // 负号是因为上滑(负数)应该增加体重
                            val delta = -(dragAmount / 300f)
                            onEvent(WeightEvent.AdjustNewWeight(delta))
                        }
                    }) {
                Spacer(modifier = Modifier)
                // 大字体数值
                Row(verticalAlignment = Alignment.Bottom) {
                    weightControlCardUiState.newWeight?.let {
                        Text(
                            text = "%.1f".format(it),
                            modifier = Modifier.testTag("weight_current_value"),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                // 背景装饰：模拟滚轮刻度
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxHeight()
                        .alpha(0.2f)
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    // 简单的刻度线
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // 确认按钮 (带动画)
            AnimatedVisibility(
                visible = weightControlCardUiState.showConfirm,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Button(
                    onClick = { onEvent(WeightEvent.AddWeight) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("weight_add_record_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.MonitorWeight, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("添加新记录: %.1f 斤".format(weightControlCardUiState.newWeight))
                }
            }

            if (!weightControlCardUiState.showConfirm && weightControlCardUiState.isTodayRecorded) {
                Text(
                    "保持这种势头！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview
@Composable
private fun WeightViewCardOldPreview() {
    WeightControlCard(
        weightControlCardUiState = WeightControlCardUiState(
            isTodayRecorded = false,
            newWeight = 70.0,
            lastDateText = "2025-11-01",
            showConfirm = true
        )
    )
}

@Preview
@Composable
private fun WeightViewCardTodayPreview() {
    WeightControlCard(
        weightControlCardUiState = WeightControlCardUiState(
            isTodayRecorded = true,
            newWeight = 71.0,
            lastDateText = "2025-11-01",
            showConfirm = false
        )
    )
}
