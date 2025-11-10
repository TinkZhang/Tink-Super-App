package app.tinks.tink.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun WeightScreen(viewModel: WeightViewModel = hiltViewModel(),
                 modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeightScreen(
        weightOfToday = uiState.weightOfToday,
        allWeights = uiState.weights,
        isLoading = uiState.isLoading,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun WeightScreen(
    weightOfToday: WeightRecord?,
    allWeights: List<WeightRecord>,
    isLoading: Boolean = false,
    onEvent: (WeightEvent) -> Unit = {},
) {

    LaunchedEffect(Unit) {
        onEvent(WeightEvent.RefreshWeightList)
    }

    Column {
        Button(onClick = { onEvent(WeightEvent.AddWeight(123.0)) }) {
            Text("添加 70.5kg")
        }

        LazyColumn {
            items(allWeights) { record ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(record.weight.toString())
                    Text(record.createdAt ?: "")
                    IconButton(onClick = {
                        record.id?.let { onEvent(WeightEvent.DeleteWeight(it)) }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }
        }
    }
}
