package app.tinks.tink.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.R
import app.tinks.tink.book.BookPageFormat
import app.tinks.tink.ui.theme.TinkTheme
import coil.compose.AsyncImage

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onAddTime: () -> Unit = {},
    onNewDiary: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onAddTime = onAddTime,
        onNewDiary = onNewDiary,
    )
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit = {},
    onAddTime: () -> Unit = {},
    onNewDiary: () -> Unit = {},
) {
    if (state.showReadKeeperProgressDialog) {
        ReadKeeperProgressDialog(
            book = state.snapshot.readKeeperBook,
            value = state.readKeeperProgressInput,
            onValueChange = { onEvent(HomeEvent.UpdateReadKeeperProgressInput(it)) },
            onDismiss = { onEvent(HomeEvent.DismissReadKeeperProgress) },
            onSave = { onEvent(HomeEvent.SaveReadKeeperProgress) },
        )
    }

    val cells = buildHomeCells(state.snapshot)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_dashboard_grid"),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cells, key = { it }) { cell ->
            when (cell) {
                HomeCell.Merriam -> MerriamHomeCell(
                    enabled = state.snapshot.merriamLatest != null,
                    onEvent = onEvent,
                )
                HomeCell.Time -> TimeHomeCell(onAddTime)
                HomeCell.ReadKeeper -> ReadKeeperHomeCell(
                    book = state.snapshot.readKeeperBook,
                    onToggleSession = { onEvent(HomeEvent.ToggleReadKeeperSession) },
                    onUpdate = { onEvent(HomeEvent.OpenReadKeeperProgress) },
                )
                HomeCell.DiaryLoom -> DiaryLoomHomeCell(onNewDiary)
                HomeCell.Weight -> WeightHomeCell(onAdd = { onEvent(HomeEvent.AddWeight) })
                HomeCell.Haircut -> HaircutHomeCell(days = state.snapshot.haircutDays ?: 0)
            }
        }
    }
}

@Composable
private fun MerriamHomeCell(
    enabled: Boolean,
    onEvent: (HomeEvent) -> Unit,
) {
    HomeCellCard(
        title = "M-W Builder",
        icon = { Icon(Icons.Filled.Bookmark, contentDescription = null) },
        testTag = "home_merriam_cell",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = { onEvent(HomeEvent.CompleteMerriamWords(4)) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) {
                Text("+4")
            }
            FilledTonalButton(
                onClick = { onEvent(HomeEvent.CompleteMerriamWords(6)) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) {
                Text("+6")
            }
        }
    }
}

@Composable
private fun TimeHomeCell(onAddTime: () -> Unit) {
    HomeCellCard(
        title = "Time",
        icon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
        testTag = "home_time_cell",
    ) {
        Button(onClick = onAddTime, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add")
        }
    }
}

@Composable
private fun ReadKeeperHomeCell(
    book: HomeReadKeeperBook?,
    onToggleSession: () -> Unit,
    onUpdate: () -> Unit,
) {
    HomeCellCard(
        title = "ReadKeeper",
        icon = {
            Image(
                painter = painterResource(R.drawable.ic_readkeeperlogo),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        },
        testTag = "home_readkeeper_cell",
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 86.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (book?.coverUrl.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.ic_readkeeperlogo),
                            contentDescription = book?.title ?: "ReadKeeper",
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = onToggleSession,
                enabled = book != null,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (book?.hasActiveSession == true) "Stop" else "Start")
            }
            OutlinedButton(
                onClick = onUpdate,
                enabled = book != null,
                modifier = Modifier.weight(1f),
            ) {
                Text("Update")
            }
        }
    }
}

@Composable
private fun DiaryLoomHomeCell(onNewDiary: () -> Unit) {
    HomeCellCard(
        title = "DiaryLoom",
        icon = {
            Image(
                painter = painterResource(R.drawable.ic_diaryloom_logo),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        },
        testTag = "home_diaryloom_cell",
    ) {
        Button(onClick = onNewDiary, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("New diary")
        }
    }
}

@Composable
private fun WeightHomeCell(onAdd: () -> Unit) {
    HomeCellCard(
        title = "Weight",
        icon = { Icon(Icons.Outlined.MonitorWeight, contentDescription = null) },
        testTag = "home_weight_cell",
    ) {
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add")
        }
    }
}

@Composable
private fun HaircutHomeCell(days: Int) {
    HomeCellCard(
        title = "理发",
        icon = { Icon(Icons.Outlined.ContentCut, contentDescription = null) },
        testTag = "home_haircut_cell",
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            "$days days",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HomeCellCard(
    title: String,
    icon: @Composable () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 124.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        icon()
                    }
                }
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            content()
        }
    }
}

@Composable
private fun ReadKeeperProgressDialog(
    book: HomeReadKeeperBook?,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val targetLabel = book?.progressTargetLabel ?: "Progress"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update ReadKeeper") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(targetLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun buildHomeCells(snapshot: HomeSnapshot): List<HomeCell> =
    buildList {
        add(HomeCell.Merriam)
        add(HomeCell.Time)
        add(HomeCell.ReadKeeper)
        add(HomeCell.DiaryLoom)
        add(HomeCell.Weight)
        if (snapshot.showHaircutReminder) add(HomeCell.Haircut)
    }

private enum class HomeCell {
    Merriam,
    Time,
    ReadKeeper,
    DiaryLoom,
    Weight,
    Haircut,
}

@Preview(showBackground = true, widthDp = 400, heightDp = 740)
@Composable
private fun HomeScreenPreview() {
    TinkTheme(dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(
                snapshot = HomeSnapshot(
                    merriamLatest = 128,
                    readKeeperBook = HomeReadKeeperBook(
                        id = 1,
                        title = "The Left Hand of Darkness",
                        coverUrl = null,
                        pageFormat = BookPageFormat.Page,
                        currentPage = 120,
                        progressPercentage = null,
                        pages = 360,
                        sessionStartedAt = null,
                        sessionStartPage = null,
                        sessionStartProgressPercentage = null,
                    ),
                    haircutDays = 42,
                    weightValue = 141.2,
                    weightRecordedAt = 1779177600000L,
                )
            ),
        )
    }
}
