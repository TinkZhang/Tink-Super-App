package app.tinks.tink.weight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.weight.data.Weight
import app.tinks.tink.weight.ui.WeightControlCard

@Composable
fun WeightScreen(viewModel: WeightViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeightScreen(
        weightControlCardUiState = uiState.weightControlCardUiState,
        isLoading = uiState.isLoading,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun WeightScreen(
    weightControlCardUiState: WeightControlCardUiState,
    isLoading: Boolean = false,
    onEvent: (WeightEvent) -> Unit = {},
) {

    LaunchedEffect(Unit) {
        onEvent(WeightEvent.RefreshWeightList)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp) // 防止底部内容被遮挡
    ) {
        // 1. 核心操作卡片
        item { WeightControlCard(weightControlCardUiState, onEvent = onEvent) }

//        // 2. 趋势分析卡片
//        item { TrendChartCard(viewModel) }
//
//        // 3. BMI 概览卡片
//        item { BmiInsightCard(viewModel) }
//
//        // 4. 底部历史入口
//        item {
//            Button(
//                onClick = { /* TODO: Navigate to full history */ },
//                modifier = Modifier.fillMaxWidth(),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
//                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
//                )
//            ) {
//                Icon(Icons.Filled.History, contentDescription = null)
//                Spacer(Modifier.width(8.dp))
//                Text("查看所有历史记录")
//                Spacer(Modifier.weight(1f))
//                Icon(Icons.Filled.ChevronRight, contentDescription = null)
//            }
//        }
    }
}
//
//@Preview
//@Composable
//private fun WeightScreenPreview() {
//    WeightScreen(
//        weightOfToday = null,
//        allWeights = listOf
//    )
//}
