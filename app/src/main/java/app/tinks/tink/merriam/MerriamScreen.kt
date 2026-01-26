package app.tinks.tink.merriam


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun MerriamScreen(viewModel: MerriamViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MerriamScreen(
        isLoading = uiState.isLoading,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun MerriamScreen(
    isLoading: Boolean = false,
    showDialog: Boolean = false,
    onEvent: (MerriamEvent) -> Unit = {},
) {

    LaunchedEffect(Unit) {
//        onEvent(ZiEvent.Refresh)
    }

    if (showDialog) {
//        EditRootDialog(
//            onDismiss = { onEvent(ZiEvent.DismissDialog) },
//            onConfirm = {}
//        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun EditRootDialog(
    onDismiss: () -> Unit = {},
    onConfirm: (price: Int, shopName: String, date: LocalDate) -> Unit,
) {
    var shopName by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    // 获取今天的日期
    val today = remember {
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    var selectedDate by remember { mutableStateOf(today) }

    // 将 LocalDate 转换为毫秒（用于 DatePicker）
    val selectedDateMillis = remember(selectedDate) {
        val instant = Instant.parse("${selectedDate}T00:00:00Z")
        instant.toEpochMilliseconds()
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC).date
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加理发记录") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("理发店名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("价格（元）") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = selectedDate.toString(),
                    onValueChange = { },
                    label = { Text("日期") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text("选择")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toIntOrNull() ?: 0
                    if (shopName.isNotBlank() && price > 0) {
                        onConfirm(price, shopName, selectedDate)
                    }
                },
                enabled = shopName.isNotBlank() && priceText.toIntOrNull() != null && (priceText.toIntOrNull()
                    ?: 0) > 0
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Preview
@Composable
private fun MerriamScreenPreview() {
    MerriamScreen()
}