package app.tinks.tink.time

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun TimeScreen(viewModel: TimeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TimeScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TimeScreen(
    state: TimeUiState,
    onEvent: (TimeEvent) -> Unit,
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        DateOnlyPickerDialog(
            initialDate = state.startDate,
            onDismiss = { showStartDatePicker = false },
            onConfirm = {
                onEvent(TimeEvent.UpdateStartDate(it))
                showStartDatePicker = false
            },
        )
    }

    if (showEndDatePicker) {
        DateOnlyPickerDialog(
            initialDate = state.endDate,
            onDismiss = { showEndDatePicker = false },
            onConfirm = {
                onEvent(TimeEvent.UpdateEndDate(it))
                showEndDatePicker = false
            },
        )
    }

    if (state.showEditor) {
        val editingId = state.editor.editingId
        TimeEntryDialog(
            editor = state.editor,
            labels = state.labels.filter { it.type == state.editor.type },
            isSaving = state.isSaving,
            isDeleting = editingId != null && editingId in state.deletingIds,
            onDismiss = { onEvent(TimeEvent.DismissDialog) },
            onTitleChange = { onEvent(TimeEvent.UpdateTitle(it)) },
            onDescriptionChange = { onEvent(TimeEvent.UpdateDescription(it)) },
            onTypeChange = { onEvent(TimeEvent.UpdateType(it)) },
            onApplyLabel = { onEvent(TimeEvent.ApplyLabel(it)) },
            onManageLabels = { onEvent(TimeEvent.OpenLabelManager) },
            onStartChange = { onEvent(TimeEvent.UpdateStartTime(it)) },
            onEndChange = { onEvent(TimeEvent.UpdateEndTime(it)) },
            onDurationClick = { onEvent(TimeEvent.ApplyDurationMinutes(it)) },
            onSubmit = { onEvent(TimeEvent.SaveEntry) },
            onDelete = editingId?.let { id ->
                {
                    onEvent(TimeEvent.DeleteEntry(id))
                    onEvent(TimeEvent.DismissDialog)
                }
            },
        )
    }

    if (state.showLabelManager) {
        TimeLabelManagerDialog(
            labels = state.labels,
            manager = state.labelManager,
            onDismiss = { onEvent(TimeEvent.DismissLabelManager) },
            onTypeChange = { onEvent(TimeEvent.UpdateLabelManagerType(it)) },
            onDraftChange = { onEvent(TimeEvent.UpdateLabelDraft(it)) },
            onEditLabel = { onEvent(TimeEvent.EditLabel(it)) },
            onSave = { onEvent(TimeEvent.SaveLabel) },
            onDelete = { onEvent(TimeEvent.DeleteLabel(it)) },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val refreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { onEvent(TimeEvent.Refresh) },
            modifier = Modifier.fillMaxSize(),
            state = refreshState,
            indicator = {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            val scale =
                                if (state.isLoading) 1f else refreshState.distanceFraction.coerceIn(
                                    0f,
                                    1f
                                )
                            scaleX = scale
                            scaleY = scale
                            alpha = scale
                        }
                ) {
                    ContainedLoadingIndicator()
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyTimeContent(
                    state = state,
                    onStartDateClick = { showStartDatePicker = true },
                    onEndDateClick = { showEndDatePicker = true },
                    onEdit = { onEvent(TimeEvent.EditEntry(it)) },
                )
            }
        }

        FloatingActionButton(
            onClick = { onEvent(TimeEvent.OpenAddDialog) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add time entry")
        }
    }
}

@Composable
private fun LazyTimeContent(
    state: TimeUiState,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onEdit: (TimeEntry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("date_range") {
            DateRangeCard(
                startDate = state.startDate,
                endDate = state.endDate,
                onStartDateClick = onStartDateClick,
                onEndDateClick = onEndDateClick,
            )
        }

        item("statistics") {
            StatisticsCard(stats = state.statistics)
        }

        if (state.entriesByDay.isEmpty()) {
            item("empty") {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No time entries in selected range.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        items(
            items = state.entriesByDay,
            key = { it.day.toString() },
        ) { dayEntries ->
            DayEntriesCard(
                dayEntries = dayEntries,
                deletingIds = state.deletingIds,
                onEdit = onEdit,
            )
        }
    }
}

@Composable
private fun DateRangeCard(
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DateRangeTextField(
            label = "Start",
            value = startDate.format(DATE_FORMATTER),
            onClick = onStartDateClick,
            testTag = "time_start_date_field",
            modifier = Modifier.weight(1f),
        )
        DateRangeTextField(
            label = "End",
            value = endDate.format(DATE_FORMATTER),
            onClick = onEndDateClick,
            testTag = "time_end_date_field",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DateRangeTextField(
    label: String,
    value: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.testTag(testTag)) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@Composable
private fun StatisticsCard(stats: List<TimeStatistic>) {
    val visibleStats = stats.filter { it.duration > 0 }.sortedByDescending { it.duration }
    val total = visibleStats.sumOf { it.duration }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        if (visibleStats.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DonutChart(stats = emptyList(), total = 0L)
                Text(
                    text = "No tracked duration in this range.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DonutChart(stats = visibleStats, total = total)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = formatDuration(total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    visibleStats.take(4).forEach { stat ->
                        CompactStatLegend(stat = stat)
                    }
                    if (visibleStats.size > 4) {
                        Text(
                            text = "+${visibleStats.size - 4}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    stats: List<TimeStatistic>,
    total: Long,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier = Modifier.size(120.dp)) {
        val strokeWidth = 18.dp.toPx()
        val inset = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        if (total <= 0L) return@Canvas

        var startAngle = -90f
        stats.forEach { stat ->
            val sweep = (stat.duration.toFloat() / total.toFloat()) * 360f
            drawArc(
                color = categoryOf(stat.type).color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun CompactStatLegend(stat: TimeStatistic) {
    val category = categoryOf(stat.type)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(category.color)
        )
        Text(
            text = category.categoryName(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatDuration(stat.duration),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DayEntriesCard(
    dayEntries: TimeDayEntries,
    deletingIds: Set<Long>,
    onEdit: (TimeEntry) -> Unit,
) {
    val dayLabel = dayEntries.day.format(DAY_HEADER_FORMATTER)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            dayEntries.entries.forEachIndexed { index, entry ->
                TimeEntryRow(
                    entry = entry,
                    isDeleting = entry.id in deletingIds,
                    onEdit = onEdit,
                )
                if (index < dayEntries.entries.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TimeEntryRow(
    entry: TimeEntry,
    isDeleting: Boolean,
    onEdit: (TimeEntry) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val start = entry.start.atZoneSameInstant(zone)
    val end = entry.end.atZoneSameInstant(zone)
    val category = categoryOf(entry.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDeleting) { onEdit(entry) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = start.toLocalTime().format(TIME_FORMATTER),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "|",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = end.toLocalTime().format(TIME_FORMATTER),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(category.color)
                )
                Text(
                    text = "  ${category.categoryName()}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatDurationMinutes(
                        java.time.Duration.between(start, end).toMinutes().coerceAtLeast(0)
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (!entry.description.isNullOrBlank()) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (isDeleting) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.CenterVertically),
                strokeWidth = 2.dp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeEntryDialog(
    editor: TimeEditorState,
    labels: List<TimeLabel>,
    isSaving: Boolean,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTypeChange: (Int) -> Unit,
    onApplyLabel: (TimeLabel) -> Unit,
    onManageLabels: () -> Unit,
    onStartChange: (LocalDateTime) -> Unit,
    onEndChange: (LocalDateTime) -> Unit,
    onDurationClick: (Long) -> Unit,
    onSubmit: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    if (showStartPicker) {
        DateTimePickerDialog(
            initialDateTime = editor.start,
            onDismiss = { showStartPicker = false },
            onConfirm = {
                onStartChange(it)
                showStartPicker = false
            },
        )
    }

    if (showEndPicker) {
        DateTimePickerDialog(
            initialDateTime = editor.end,
            onDismiss = { showEndPicker = false },
            onConfirm = {
                onEndChange(it)
                showEndPicker = false
            },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(if (editor.editingId == null) "Add Time Entry" else "Edit Time Entry")
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        },
                    )
                },
                bottomBar = {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (onDelete != null) {
                                OutlinedButton(
                                    onClick = onDelete,
                                    enabled = !isDeleting && !isSaving,
                                ) {
                                    if (isDeleting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text("  Delete")
                                    }
                                }
                            }
                            Button(
                                onClick = onSubmit,
                                enabled = editor.isValid() && !isSaving && !isDeleting,
                                modifier = Modifier.weight(1f),
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text(if (editor.editingId == null) "Add" else "Save")
                                }
                            }
                        }
                    }
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = editor.title,
                        onValueChange = onTitleChange,
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Labels",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onManageLabels) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit")
                            }
                        }
                        if (labels.isEmpty()) {
                            Text(
                                text = "No labels for ${categoryOf(editor.type).categoryName()} yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                labels.forEach { label ->
                                    AssistChip(
                                        onClick = { onApplyLabel(label) },
                                        label = { Text(label.name) },
                                    )
                                }
                            }
                        }
                    }

                    Box {
                        OutlinedTextField(
                            value = categoryOf(editor.type).categoryName(),
                            onValueChange = {},
                            label = { Text("Type") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTypeMenu = true },
                            trailingIcon = {
                                IconButton(onClick = { showTypeMenu = true }) {
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = "Select type"
                                    )
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = showTypeMenu,
                            onDismissRequest = { showTypeMenu = false },
                        ) {
                            TIME_CATEGORIES.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(category.color)
                                            )
                                            Text(category.categoryName())
                                        }
                                    },
                                    onClick = {
                                        onTypeChange(category.id)
                                        showTypeMenu = false
                                    },
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null)
                        Text("  Start: ${editor.start.format(DATE_TIME_FORMATTER)}")
                    }

                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null)
                        Text("  End: ${editor.end.format(DATE_TIME_FORMATTER)}")
                    }

                    Text(
                        text = "Quick duration (minutes):",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(30L, 60L, 90L, 120L).forEach { option ->
                            AssistChip(
                                onClick = { onDurationClick(option) },
                                label = { Text("${option}m") },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editor.description,
                        onValueChange = onDescriptionChange,
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                    )

                    if (!editor.end.isAfter(editor.start)) {
                        Text(
                            text = "End time must be after start time.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeLabelManagerDialog(
    labels: List<TimeLabel>,
    manager: TimeLabelManagerState,
    onDismiss: () -> Unit,
    onTypeChange: (Int) -> Unit,
    onDraftChange: (String) -> Unit,
    onEditLabel: (TimeLabel) -> Unit,
    onSave: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    var showTypeMenu by remember { mutableStateOf(false) }
    val visibleLabels = labels
        .filter { it.type == manager.selectedType }
        .sortedWith(compareBy<TimeLabel> { it.sortOrder }.thenBy { it.name })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Time Labels") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        },
                    )
                },
                bottomBar = {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = manager.draftName,
                                onValueChange = onDraftChange,
                                label = { Text(if (manager.editingId == null) "New label" else "Edit label") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = onSave,
                                enabled = manager.isValid() && !manager.isSaving,
                            ) {
                                if (manager.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text(if (manager.editingId == null) "Add" else "Save")
                                }
                            }
                        }
                    }
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box {
                        OutlinedTextField(
                            value = categoryOf(manager.selectedType).categoryName(),
                            onValueChange = {},
                            label = { Text("Type") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTypeMenu = true },
                            trailingIcon = {
                                IconButton(onClick = { showTypeMenu = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select type")
                                }
                            },
                        )
                        DropdownMenu(
                            expanded = showTypeMenu,
                            onDismissRequest = { showTypeMenu = false },
                        ) {
                            TIME_CATEGORIES.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(category.color)
                                            )
                                            Text(category.categoryName())
                                        }
                                    },
                                    onClick = {
                                        onTypeChange(category.id)
                                        showTypeMenu = false
                                    },
                                )
                            }
                        }
                    }

                    Text(
                        text = "Tap a label to edit it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (visibleLabels.isEmpty()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No labels yet for ${categoryOf(manager.selectedType).categoryName()}.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            items(
                                items = visibleLabels,
                                key = { it.id },
                            ) { label ->
                                LabelRow(
                                    label = label,
                                    isEditing = manager.editingId == label.id,
                                    isDeleting = label.id in manager.deletingIds,
                                    onEdit = { onEditLabel(label) },
                                    onDelete = { onDelete(label.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabelRow(
    label: TimeLabel,
    isEditing: Boolean,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InputChip(
                selected = isEditing,
                onClick = onEdit,
                label = { Text(label.name) },
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onDelete,
                enabled = !isDeleting,
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete label")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateOnlyPickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = datePickerState.selectedDateMillis
                if (millis != null) {
                    val selected = Instant.ofEpochMilli(millis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                    onConfirm(selected)
                } else {
                    onConfirm(initialDate)
                }
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

private enum class PickerStep {
    DATE,
    TIME,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerDialog(
    initialDateTime: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
) {
    var step by remember { mutableStateOf(PickerStep.DATE) }
    var selectedDate by remember { mutableStateOf(initialDateTime.toLocalDate()) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateTime.toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = initialDateTime.hour,
        initialMinute = initialDateTime.minute,
        is24Hour = true,
    )

    if (step == PickerStep.DATE) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    step = PickerStep.TIME
                }) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pick Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    val localDateTime = LocalDateTime.of(
                        selectedDate,
                        LocalTime.of(timePickerState.hour, timePickerState.minute),
                    )
                    onConfirm(localDateTime)
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
        )
    }
}

private data class TimeCategory(
    val id: Int,
    val name: String,
    val color: Color,
    val description: String,
)

private val TIME_CATEGORIES = listOf(
    TimeCategory(1, "Lavender", Color(0xFFC7CEFF), "Diary, Personal"),
    TimeCategory(2, "Sage", Color(0xFFB7DDB2), "Reading, Study"),
    TimeCategory(3, "Grape", Color(0xFFA68AD8), "Calm, Soothing"),
    TimeCategory(4, "Flamingo", Color(0xFFFF9CB3), "Freelance"),
    TimeCategory(5, "Banana", Color(0xFFFFE082), "Working"),
    TimeCategory(6, "Tangerine", Color(0xFFFFB36B), "Game, Fun with Tink"),
    TimeCategory(7, "Peacock", Color(0xFF75D7D8), "Parenting"),
    TimeCategory(8, "Graphite", Color(0xFF9E9E9E), "Leisure"),
    TimeCategory(9, "Blueberry", Color(0xFF4F6FD5), "Deep Thinking, Coding"),
    TimeCategory(10, "Basil", Color(0xFF4E8F58), "Outdoors, Nature"),
    TimeCategory(11, "Tomato", Color(0xFFE35D5B), "Important, Urgent"),
)

private fun categoryOf(id: Int): TimeCategory {
    return TIME_CATEGORIES.firstOrNull { it.id == id }
        ?: TimeCategory(id = id, name = "Type $id", color = Color.Gray, description = "Unknown")
}

private fun TimeCategory.categoryName(): String {
    return description.ifBlank { name }
}

private fun formatDuration(duration: Long): String {
    return formatDurationMinutes(duration)
}

private fun formatDurationMinutes(minutes: Long): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val remain = safeMinutes % 60
    return if (hours > 0) "${hours}h ${remain}m" else "${remain}m"
}

private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
private val DAY_HEADER_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d")
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
