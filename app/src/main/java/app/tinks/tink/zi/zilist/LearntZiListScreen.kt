package app.tinks.tink.zi.zilist


import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.zi.SawtoothCard
import app.tinks.tink.zi.Zi

@Composable
fun LearntZiListScreen(viewModel: LearntZiListViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LearntZiListScreen(
        zis = uiState.zis,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun LearntZiListScreen(
    zis: List<Zi> = emptyList(),
    onEvent: (ZiListEvent) -> Unit = {},
) {

    LaunchedEffect(Unit) {
        onEvent(ZiListEvent.Refresh)
    }

    LazyVerticalGrid(columns = GridCells.Fixed(3)) {
        items(zis) { zi ->
            SawtoothCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = zi.zi,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                )
            }
        }
    }
}


@Preview
@Composable
private fun LearntZiListScreenPreview() {
    LearntZiListScreen()
}