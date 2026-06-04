package app.tinks.tink.lottery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.ui.theme.TinkTheme
import java.time.Instant

@Composable
fun LotteryScreen(
    viewModel: LotteryViewModel,
    onOpenHistoryStats: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.onEvent(LotteryEvent.UseCapturedTicketPreview)
        }
    }

    LotteryScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onOpenHistoryStats = onOpenHistoryStats,
        onRequestCapture = { cameraLauncher.launch(null) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LotteryScreen(
    state: LotteryUiState,
    onEvent: (LotteryEvent) -> Unit = {},
    onOpenHistoryStats: () -> Unit = {},
    onRequestCapture: () -> Unit = {},
) {
    val refreshState = rememberPullToRefreshState()

    when {
        state.luckyOutcome != null -> LotteryResultHero(
            outcome = state.luckyOutcome,
            onDone = { onEvent(LotteryEvent.DismissHero) },
        )
        state.draft != null -> LotteryReviewScreen(
            draft = state.draft,
            isSaving = state.isLoading,
            onEvent = onEvent,
            onRetakePhoto = onRequestCapture,
        )
        else -> Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onRequestCapture,
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                    text = { Text("Add") },
                    modifier = Modifier.testTag("lottery_add_fab"),
                )
            }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { onEvent(LotteryEvent.Refresh) },
                state = refreshState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("lottery_screen"),
            ) {
                LotteryDashboard(
                    state = state,
                    onEvent = onEvent,
                    onOpenHistoryStats = onOpenHistoryStats,
                )
            }
        }
    }
}

@Composable
fun LotteryHistoryStatsScreen(viewModel: LotteryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LotteryHistoryStatsScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
internal fun LotteryHistoryStatsScreen(
    state: LotteryUiState,
    onEvent: (LotteryEvent) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("lottery_history_stats_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
    ) {
        item("stats") {
            LotteryStatsPanel(state.stats)
        }

        item("history_header") {
            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (state.historyTickets.isEmpty()) {
            item("empty") {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lottery_history_empty"),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.LocalActivity, contentDescription = null)
                        Text("No lottery history yet.", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        } else {
            items(state.historyTickets, key = { it.ticket.id }) { ticket ->
                LotteryHistoryItem(
                    ticket = ticket,
                    onDelete = { onEvent(LotteryEvent.DeleteLottery(ticket.ticket.id)) },
                )
            }
        }
    }
}

@Composable
private fun LotteryDashboard(
    state: LotteryUiState,
    onEvent: (LotteryEvent) -> Unit,
    onOpenHistoryStats: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
    ) {
        item("active") {
            val active = state.activeTicket
            if (active == null) {
                LotteryEmptyActiveCard()
            } else {
                LotteryActiveTicketCard(
                    ticket = active,
                    onReveal = { onEvent(LotteryEvent.CheckLottery(active.ticket.id)) },
                )
            }
        }

        item("history_stats") {
            LotteryHistoryStatsButton(onClick = onOpenHistoryStats)
        }
    }
}

@Composable
private fun LotteryEmptyActiveCard() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lottery_current_ticket_card"),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Filled.AddAPhoto,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "No active lottery",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Capture a 大乐透 ticket and review the parsed JSON before saving.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LotteryActiveTicketCard(
    ticket: LotteryTicketUiState,
    onReveal: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lottery_current_ticket_card"),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "第${ticket.ticket.issueId}期",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = ticket.ticket.type,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LotteryStatusChip(ticket.status)
            }

            LotteryNumbersBlock(ticket.ticket.numbers, label = "My numbers")

            Text(
                text = "Reveal time ${ticket.revealTimeText}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ticket.ticket.result?.let { result ->
                LotteryNumbersBlock(result.numbers, label = "Winning numbers")
            }

            ticket.matchSummary?.let { summary ->
                Text(
                    text = "前区 ${summary.frontMatches} / 后区 ${summary.backMatches} · ${summary.prizeTier}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (summary.isWinning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            if (ticket.canReveal) {
                Button(
                    onClick = onReveal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lottery_reveal_button_${ticket.ticket.id}"),
                ) {
                    Icon(Icons.Filled.Celebration, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("开奖")
                }
            }
        }
    }
}

@Composable
private fun LotteryStatusChip(status: LotteryTicketStatus) {
    val label = when (status) {
        LotteryTicketStatus.Pending -> "等待开奖"
        LotteryTicketStatus.Ready -> "可开奖"
        LotteryTicketStatus.Revealed -> "已开奖"
    }
    AssistChip(onClick = {}, label = { Text(label) })
}

@Composable
private fun LotteryNumbersBlock(numbers: LotteryNumbers, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            numbers.front.forEach { number ->
                NumberBall(
                    number = number,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            numbers.back.forEach { number ->
                NumberBall(
                    number = number,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun NumberBall(
    number: Int,
    containerColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}

@Composable
private fun LotteryHistoryStatsButton(onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lottery_history_stats_button"),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(34.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "History & stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Past tickets, wins, and prize distribution",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun LotteryReviewScreen(
    draft: LotteryDraft,
    isSaving: Boolean,
    onEvent: (LotteryEvent) -> Unit,
    onRetakePhoto: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("lottery_review_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
    ) {
        item("header") {
            Text(
                text = "Review ticket",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item("type") {
            OutlinedTextField(
                value = draft.type,
                onValueChange = {},
                enabled = false,
                label = { Text("Type") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item("issue") {
            OutlinedTextField(
                value = draft.issueId,
                onValueChange = { onEvent(LotteryEvent.UpdateDraftIssueId(it)) },
                label = { Text("Issue id") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item("front") {
            OutlinedTextField(
                value = draft.frontNumbersText,
                onValueChange = { onEvent(LotteryEvent.UpdateDraftFrontNumbers(it)) },
                label = { Text("Front numbers") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item("back") {
            OutlinedTextField(
                value = draft.backNumbersText,
                onValueChange = { onEvent(LotteryEvent.UpdateDraftBackNumbers(it)) },
                label = { Text("Back numbers") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item("reveal") {
            OutlinedTextField(
                value = draft.revealTimeText,
                onValueChange = { onEvent(LotteryEvent.UpdateDraftRevealTime(it)) },
                label = { Text("Reveal time") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        draft.parseError?.let { error ->
            item("error") {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item("json") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    text = draft.jsonPreview,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isSaving) {
            item("progress") {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        item("actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        onEvent(LotteryEvent.RetakePhoto)
                        onRetakePhoto()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lottery_review_retake_button"),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Retake")
                }
                FilledTonalButton(
                    onClick = { onEvent(LotteryEvent.DismissReview) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lottery_review_manual_edit_button"),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onEvent(LotteryEvent.SaveDraft) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lottery_review_save_button"),
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun LotteryResultHero(
    outcome: LotteryCheckOutcome,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("lottery_result_hero"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Celebration,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = outcome.prizeTier,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "第${outcome.lottery.issueId}期",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        LotteryNumbersBlock(outcome.lottery.numbers, label = "My numbers")
        Spacer(Modifier.height(16.dp))
        LotteryNumbersBlock(outcome.result.numbers, label = "Winning numbers")
        Spacer(Modifier.height(24.dp))
        Text(
            text = "前区 ${outcome.frontMatchCount} / 后区 ${outcome.backMatchCount}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone) {
            Text("Done")
        }
    }
}

@Composable
private fun LotteryStatsPanel(stats: LotteryStatsUiState) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lottery_stats_panel"),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Total", stats.totalTickets.toString(), Modifier.weight(1f))
                StatPill("Pending", stats.pendingTickets.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Revealed", stats.revealedTickets.toString(), Modifier.weight(1f))
                StatPill("Wins", stats.winningTickets.toString(), Modifier.weight(1f))
            }
            StatPill("Best prize", stats.bestPrizeTier, Modifier.fillMaxWidth())
            if (stats.prizeDistribution.isNotEmpty()) {
                stats.prizeDistribution.forEach { (tier, count) ->
                    Text(
                        text = "$tier · $count",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun LotteryHistoryItem(
    ticket: LotteryTicketUiState,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lottery_ticket_card_${ticket.ticket.id}"),
        shape = RoundedCornerShape(8.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = "第${ticket.ticket.issueId}期",
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Column {
                    Text(ticket.revealTimeText)
                    Text(ticket.matchSummary?.prizeTier ?: statusText(ticket.status))
                    CompactNumbersRow(ticket.ticket.numbers)
                }
            },
            leadingContent = {
                Icon(Icons.Filled.LocalActivity, contentDescription = null)
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete lottery")
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        )
    }
}

@Composable
private fun CompactNumbersRow(numbers: LotteryNumbers) {
    Text(
        text = numbers.front.joinToString(" ") { it.toString().padStart(2, '0') } +
            " + " +
            numbers.back.joinToString(" ") { it.toString().padStart(2, '0') },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun statusText(status: LotteryTicketStatus): String =
    when (status) {
        LotteryTicketStatus.Pending -> "等待开奖"
        LotteryTicketStatus.Ready -> "可开奖"
        LotteryTicketStatus.Revealed -> "已开奖"
    }

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LotteryScreenPreview() {
    TinkTheme(dynamicColor = false) {
        LotteryScreen(
            state = LotteryUiState(
                isLoading = false,
                activeTicket = sampleTicketUiState(),
                historyTickets = listOf(sampleTicketUiState()),
                stats = LotteryStatsUiState(1, 0, 0, 0, "暂无", emptyList()),
                draft = null,
                luckyOutcome = null,
            )
        )
    }
}

private fun sampleTicketUiState(): LotteryTicketUiState {
    val ticket = LotteryTicket(
        id = 1,
        type = LOTTERY_TYPE_DA_LE_TOU,
        issueId = "21126",
        numbers = LotteryNumbers(listOf(1, 11, 12, 34, 35), listOf(9, 12)),
        revealTime = Instant.parse("2021-11-03T12:30:00Z"),
        capturedImageUri = null,
        checked = false,
        checkedAt = null,
        resultId = null,
        prizeTier = null,
        frontMatchCount = null,
        backMatchCount = null,
        result = null,
    )
    return LotteryTicketUiState(
        ticket = ticket,
        status = LotteryTicketStatus.Ready,
        revealTimeText = "2021-11-03 12:30:00",
        checkedTimeText = null,
        matchSummary = null,
    )
}
