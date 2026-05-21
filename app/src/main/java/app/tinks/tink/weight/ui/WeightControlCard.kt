package app.tinks.tink.weight.ui

import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tinks.tink.weight.WeightControlCardUiState
import app.tinks.tink.weight.WeightEvent

@Composable
fun WeightControlCard(
    weightControlCardUiState: WeightControlCardUiState,
    onEvent: (WeightEvent) -> Unit = {},
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weight_control_card"),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.MonitorWeight, contentDescription = null)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "当前体重",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    WeightStatusChip(weightControlCardUiState)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
                    .testTag("weight_drag_area")
                    .semantics {
                        contentDescription = "weight adjustment area"
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = -(dragAmount / 300f)
                            onEvent(WeightEvent.AdjustNewWeight(delta))
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        val currentWeight = weightControlCardUiState.newWeight
                        if (currentWeight == null) {
                            Spacer(
                                modifier = Modifier
                                    .height(96.dp)
                                    .fillMaxWidth()
                                    .testTag("weight_current_value")
                            )
                        } else {
                            Text(
                                text = "%.1f".format(Locale.current.platformLocale, currentWeight),
                                modifier = Modifier
                                    .testTag("weight_current_value")
                                    .semantics {
                                        contentDescription = "current weight %.1f".format(
                                            Locale.current.platformLocale,
                                            currentWeight,
                                        )
                                    },
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 76.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "斤",
                                modifier = Modifier.padding(bottom = 14.dp),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        if (weightControlCardUiState.newWeight == null) {
                            "还没有体重记录"
                        } else {
                            "上下滑动右侧控件调整"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                WeightAdjustmentRail(
                    onIncrease = { onEvent(WeightEvent.AdjustNewWeight(0.1f)) },
                    onDecrease = { onEvent(WeightEvent.AdjustNewWeight(-0.1f)) },
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = weightControlCardUiState.showConfirm,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Button(
                    onClick = { onEvent(WeightEvent.AddWeight) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("weight_add_record_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("记录 %.1f 斤".format(weightControlCardUiState.newWeight))
                }
            }
        }
    }
}

@Composable
private fun WeightStatusChip(weightControlCardUiState: WeightControlCardUiState) {
    if (!weightControlCardUiState.isTodayRecorded) {
        AssistChip(
            onClick = {},
            label = { Text("上次记录: ${weightControlCardUiState.lastDateText}") },
            leadingIcon = {
                Icon(Icons.Outlined.Warning, null)
            },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = MaterialTheme.colorScheme.error,
                leadingIconContentColor = MaterialTheme.colorScheme.error,
            ),
            border = AssistChipDefaults.assistChipBorder(
                enabled = true,
                borderColor = MaterialTheme.colorScheme.error,
            ),
        )
    } else {
        AssistChip(
            onClick = {},
            label = { Text("今日已记录") },
            leadingIcon = {
                Icon(Icons.Filled.Check, null)
            },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = MaterialTheme.colorScheme.primary,
                leadingIconContentColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun WeightAdjustmentRail(
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(88.dp)
            .fillMaxHeight(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            FilledTonalIconButton(onClick = onIncrease) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "增加体重")
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .alpha(0.42f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                repeat(7) { index ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(if (index == 3) 0.86f else 0.58f)
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {}
                    if (index != 6) {
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            FilledTonalIconButton(onClick = onDecrease) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "减少体重")
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
