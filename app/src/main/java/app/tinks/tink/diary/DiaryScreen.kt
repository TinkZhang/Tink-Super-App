package app.tinks.tink.diary

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.R
import app.tinks.tink.ui.components.YearContributionGraph
import app.tinks.tink.ui.theme.TinkTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    onOpenDrawer: () -> Unit = {},
    newDiaryRequestId: Int = 0,
    onNewDiaryRequestConsumed: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(newDiaryRequestId) {
        if (newDiaryRequestId > 0) {
            viewModel.onEvent(DiaryEvent.ComposeNew)
            onNewDiaryRequestConsumed()
        }
    }
    DiaryScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenDrawer = onOpenDrawer,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiaryScreen(
    state: DiaryUiState,
    onEvent: (DiaryEvent) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            DiaryTopBar(
                screen = state.screen,
                onOpenDrawer = onOpenDrawer,
                onBack = { onEvent(DiaryEvent.NavigateBack) },
                onSearch = { onEvent(DiaryEvent.OpenList) },
            )
        },
        floatingActionButton = {
            if (state.screen == DiaryScreenState.Home || state.screen == DiaryScreenState.List) {
                ExtendedFloatingActionButton(
                    onClick = { onEvent(DiaryEvent.ComposeNew) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New diary") },
                    modifier = Modifier.testTag("diary_new_button"),
                )
            }
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (state.screen) {
                DiaryScreenState.Home -> DiaryHomeContent(state, onEvent)
                DiaryScreenState.List -> DiaryListContent(state, onEvent)
                is DiaryScreenState.Details -> DiaryDetailsContent(state, onEvent)
                is DiaryScreenState.Compose -> DiaryComposeContent(state, onEvent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryTopBar(
    screen: DiaryScreenState,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
) {
    val isHome = screen == DiaryScreenState.Home
    if (isHome) {
        CenterAlignedTopAppBar(
            windowInsets = WindowInsets(0.dp),
            navigationIcon = {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("diary_top_bar_menu_button")) {
                    Icon(Icons.Filled.Menu, contentDescription = "Open drawer")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_diaryloom_logo),
                        contentDescription = "DiaryLoom logo",
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("DiaryLoom")
                }
            },
            actions = {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Search diaries")
                }
            },
        )
    } else {
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("diary_top_bar_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = {
                Text(
                    when (screen) {
                        DiaryScreenState.List -> "Diaries"
                        is DiaryScreenState.Details -> "Diary"
                        is DiaryScreenState.Compose -> "Write"
                        DiaryScreenState.Home -> "DiaryLoom"
                    }
                )
            },
        )
    }
}

@Composable
private fun DiaryHomeContent(
    state: DiaryUiState,
    onEvent: (DiaryEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("weekly") {
            WeeklyRecordCard(
                data = state.weeklyRecordData,
                onWeekOffsetChange = { onEvent(DiaryEvent.UpdateWeekOffset(it)) },
            )
        }
        item("year") {
            YearlyDiaryCard(
                year = state.contributionYear,
                markedDates = state.contributionDates,
            )
        }
        if (state.recentDiaries.isNotEmpty()) {
            item("recent_header") {
                SectionHeader(
                    title = "Recent diaries",
                    action = "All",
                    onAction = { onEvent(DiaryEvent.OpenList) },
                )
            }
            items(state.recentDiaries, key = { "recent-${it.id}" }) { diary ->
                DiaryListItem(
                    diary = diary,
                    onClick = { onEvent(DiaryEvent.OpenDetails(diary.id)) },
                )
            }
        }
        if (state.drafts.isNotEmpty()) {
            item("drafts") {
                DraftSection(
                    drafts = state.drafts,
                    onDraftClick = { onEvent(DiaryEvent.ComposeExisting(it.id)) },
                )
            }
        }
    }
}

@Composable
private fun WeeklyRecordCard(
    data: WeeklyRecordData,
    onWeekOffsetChange: (Int) -> Unit,
) {
    var offset by remember { mutableIntStateOf(0) }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        offset -= 1
                        onWeekOffsetChange(offset)
                    }
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Previous week")
                }
                Text(
                    text = weekRangeLabel(offset),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = {
                        offset += 1
                        onWeekOffsetChange(offset)
                    }
                ) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Next week")
                }
            }
            if (data.records.isEmpty()) {
                Text(
                    text = "Loading weekly record...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            when (data.hasWeekSummary) {
                                true -> Modifier.background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
                                    RoundedCornerShape(8.dp),
                                )
                                false -> Modifier.background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.32f),
                                    RoundedCornerShape(8.dp),
                                )
                                null -> Modifier
                            }
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    data.records.forEach { record ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RecordDot(record.status)
                            Text(
                                text = record.date.dayOfWeek.getDisplayName(
                                    java.time.format.TextStyle.SHORT,
                                    Locale.getDefault(),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordDot(status: RecordStatus) {
    val color = when (status) {
        RecordStatus.DonePast -> MaterialTheme.colorScheme.primary
        RecordStatus.MissedPast -> MaterialTheme.colorScheme.error
        RecordStatus.DoneToday -> MaterialTheme.colorScheme.tertiary
        RecordStatus.TodoToday -> MaterialTheme.colorScheme.primaryContainer
        RecordStatus.Future -> MaterialTheme.colorScheme.surfaceVariant
    }
    val label = when (status) {
        RecordStatus.MissedPast -> "x"
        RecordStatus.TodoToday -> "!"
        else -> ""
    }
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun YearlyDiaryCard(
    year: Int,
    markedDates: Set<LocalDate>,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$year contribution",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Box(Modifier.testTag("diary_year_contribution_graph")) {
                YearContributionGraph(
                    year = year,
                    markedDates = markedDates,
                    markedColor = MaterialTheme.colorScheme.tertiary,
                )
            }
            Text(
                text = "${markedDates.size} daily entries",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (action != null) {
            TextButton(onClick = onAction) {
                Text(action)
            }
        }
    }
}

@Composable
private fun DraftSection(
    drafts: List<Diary>,
    onDraftClick: (Diary) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = "Drafts")
        drafts.forEach { draft ->
            DiaryListItem(
                diary = draft,
                titleFallback = "Untitled draft",
                onClick = { onDraftClick(draft) },
            )
        }
    }
}

@Composable
private fun DiaryListContent(
    state: DiaryUiState,
    onEvent: (DiaryEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("search") {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onEvent(DiaryEvent.SearchChanged(it)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text("Search diaries") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("diary_search_field"),
            )
        }
        if (state.filteredDiaries.isEmpty()) {
            item("empty") {
                Text(
                    text = "No diaries yet.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.filteredDiaries, key = { it.id }) { diary ->
            DiaryListItem(
                diary = diary,
                onClick = { onEvent(DiaryEvent.OpenDetails(diary.id)) },
            )
        }
    }
}

@Composable
private fun DiaryListItem(
    diary: Diary,
    modifier: Modifier = Modifier,
    titleFallback: String = "Untitled diary",
    onClick: () -> Unit,
) {
    val alpha = when (diary.type) {
        DiaryType.Year -> 1.0f
        DiaryType.Month -> 0.7f
        DiaryType.Week -> 0.4f
        DiaryType.Day -> 0.0f
    }
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (alpha == 0f) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = diary.title.ifBlank { titleFallback },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${diary.wordCount} words",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (diary.content.isNotBlank()) {
                    Text(
                        text = diary.content,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = diary.type.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = diary.displayTime(),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DiaryDetailsContent(
    state: DiaryUiState,
    onEvent: (DiaryEvent) -> Unit,
) {
    val diary = state.selectedDiary
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete && diary != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete diary?") },
            text = { Text("This removes the local DiaryLoom entry.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onEvent(DiaryEvent.DeleteDiary(diary.id))
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (diary == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Diary not found")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item("content") {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = diary.displayTime(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = diary.title.ifBlank { "Untitled diary" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = diary.content.ifBlank { "No content yet." },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = if (diary.timeEntryId == null) {
                            "Not synced to Time"
                        } else {
                            "Full-day Time event #${diary.timeEntryId}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item("actions") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onEvent(DiaryEvent.ComposeExisting(diary.id)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Edit")
                    }
                    OutlinedButton(
                        onClick = { shareDiary(context, diary) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
                }
                Button(
                    onClick = {
                        if (diary.timeEntryId == null) {
                            onEvent(DiaryEvent.SyncToTime(diary.id))
                        } else {
                            onEvent(DiaryEvent.SyncToTime(diary.id))
                        }
                    },
                    enabled = diary.id !in state.syncingIds,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (diary.id in state.syncingIds) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (diary.timeEntryId == null) "Create full-day Time event" else "Update full-day Time event")
                    }
                }
                if (diary.timeEntryId != null) {
                    OutlinedButton(
                        onClick = { onEvent(DiaryEvent.RemoveTimeEvent(diary.id)) },
                        enabled = diary.id !in state.syncingIds,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Remove Time event")
                    }
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete local diary")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryComposeContent(
    state: DiaryUiState,
    onEvent: (DiaryEvent) -> Unit,
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val draft = state.draft

    if (showStartPicker) {
        DiaryDatePickerDialog(
            initialDate = draft.startDate,
            onDismiss = { showStartPicker = false },
            onConfirm = {
                onEvent(DiaryEvent.StartDateChanged(it))
                showStartPicker = false
            },
        )
    }
    if (showEndPicker) {
        DiaryDatePickerDialog(
            initialDate = draft.endDate,
            onDismiss = { showEndPicker = false },
            onConfirm = {
                onEvent(DiaryEvent.EndDateChanged(it))
                showEndPicker = false
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { onEvent(DiaryEvent.SaveDraft) },
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save draft")
                    }
                    Button(
                        onClick = { onEvent(DiaryEvent.SaveDiary) },
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Publish")
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item("type") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DiaryType.entries.forEach { type ->
                        FilterChip(
                            selected = draft.type == type,
                            onClick = { onEvent(DiaryEvent.TypeChanged(type)) },
                            label = { Text(type.label) },
                        )
                    }
                }
            }
            item("dates") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Start ${draft.startDate.format(SHORT_DATE)}")
                    }
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("End ${draft.endDate.format(SHORT_DATE)}")
                    }
                }
            }
            item("title") {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { onEvent(DiaryEvent.TitleChanged(it)) },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diary_title_field"),
                )
            }
            item("content") {
                OutlinedTextField(
                    value = draft.content,
                    onValueChange = { onEvent(DiaryEvent.ContentChanged(it)) },
                    label = { Text("Diary") },
                    minLines = 14,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diary_content_field"),
                )
            }
            item("hint") {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text("Time sync will create a full-day event and exclude it from Time statistics.")
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    } ?: onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

private fun shareDiary(context: android.content.Context, diary: Diary) {
    val text = buildString {
        appendLine(diary.title.ifBlank { "Diary" })
        appendLine(diary.displayTime())
        appendLine()
        append(diary.content)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share diary"))
}

private val SHORT_DATE: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault())

@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun DiaryScreenPreview() {
    TinkTheme(dynamicColor = false) {
        DiaryScreen(
            state = DiaryUiState(
                recentDiaries = listOf(diarySampleDay, diarySampleWeek),
                diaries = listOf(diarySampleDay, diarySampleWeek),
                drafts = listOf(diarySampleDay.copy(title = "Draft")),
                weeklyRecordData = WeeklyRecordData(
                    hasWeekSummary = true,
                    records = (0..6).map {
                        DailyRecord(
                            date = LocalDate.of(2026, 6, 15).plusDays(it.toLong()),
                            status = if (it < 4) RecordStatus.DonePast else RecordStatus.Future,
                        )
                    },
                ),
                contributionYear = 2026,
            ),
        )
    }
}
