package app.tinks.tink.zi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.ui.components.ContentCard


@Composable
fun ZiScreen(viewModel: ZiViewModel, onNavigationEvent: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ZiScreen(
        isLoading = uiState.isLoading,
        showDialog = uiState.showDialog,
        learntZiNum = uiState.learntZiNum,
        reviewList = uiState.reviewList,
        onEvent = viewModel::onEvent,
        onNavigationEvent = onNavigationEvent,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ZiScreen(
    isLoading: Boolean = false,
    showDialog: Boolean = false,
    reviewList: List<Zi> = emptyList(),
    learntZiNum: Int = 0,
    onEvent: (ZiEvent) -> Unit = {},
    onNavigationEvent: () -> Unit = {},
) {

    LaunchedEffect(Unit) {
        onEvent(ZiEvent.Refresh)
    }
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ContentCard(
                title = "掌握汉字数量",
                showNavigation = true,
                onCardNavigation = { onNavigationEvent() }) {
                Text(
                    text = learntZiNum.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 60.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (reviewList.isNotEmpty()) {
                ContentCard(title = "复习汉字") {
                    LazyVerticalGrid(columns = GridCells.Fixed(3)) {
                        items(reviewList) { zi ->
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
            }
        }
        FloatingActionButton(
            onClick = { onEvent(ZiEvent.AddZiDialogOpen) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, "添加记录")
        }

        if (showDialog) {
            AddZiDialog(
                onDismiss = { onEvent(ZiEvent.DismissDialog) },
                onConfirm = { proficiency, zis ->
                    onEvent(
                        ZiEvent.UpdateZi(
                            zis = zis,
                            proficiency = proficiency ?: 0,
                        )
                    )
                }
            )
        }
    }
}


@Preview
@Composable
private fun ZiScreenPreview() {
    ZiScreen()
}