package app.tinks.tink.zi.zilist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.zi.SawtoothCard
import app.tinks.tink.zi.Zi
import app.tinks.tink.zi.ZiViewModel
import app.tinks.tink.zi.isReadable

@Composable
fun LearntZiListScreen(viewModel: ZiViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ZiCharacterList(
        characters = state.allZis.filter(Zi::isReadable).map(Zi::zi),
        isLoading = state.isLoading,
        failed = state.isNetworkError,
        emptyText = "还没有会读的汉字",
        tag = "zi_read_list",
        onRetry = viewModel::refreshAllData,
    )
}

@Composable
fun WrittenZiListScreen(viewModel: ZiViewModel) {
    val state by viewModel.writtenState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshWritten() }
    ZiCharacterList(
        characters = state.characters.orEmpty(),
        isLoading = state.isLoading,
        failed = state.failed,
        emptyText = "还没有会写的汉字",
        tag = "zi_written_list",
        onRetry = viewModel::refreshWritten,
    )
}

@Composable
internal fun ZiCharacterList(
    characters: List<String>,
    isLoading: Boolean,
    failed: Boolean,
    emptyText: String,
    tag: String,
    onRetry: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().testTag(tag)) {
        if (failed) {
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("加载失败，点击重试")
            }
        }
        if (characters.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                if (isLoading) CircularProgressIndicator()
                else if (!failed) Text(emptyText, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(88.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(characters, key = { it }) { character ->
                    SawtoothCard(contentPadding = PaddingValues(8.dp)) {
                        Text(
                            text = character,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
