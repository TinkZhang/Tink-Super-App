package app.tinks.tink.leetkeeper

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.R
import app.tinks.tink.ui.theme.TinkTheme

@Composable
fun LeetKeeperScreen(viewModel: LeetKeeperViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LeetKeeperScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LeetKeeperScreen(
    state: LeetKeeperUiState,
    onEvent: (LeetKeeperEvent) -> Unit = {},
) {
    val refreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { onEvent(LeetKeeperEvent.Refresh) },
        state = refreshState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("leetkeeper_screen"),
    ) {
        when {
            state.focusedProblem != null -> ProblemDetailPane(
                problem = state.focusedProblem,
                ongoingPlan = state.ongoingPlan,
                onEvent = onEvent,
            )

            state.selectedPublicPlan != null -> PublicPlanDetailPane(
                plan = state.selectedPublicPlan,
                selectedLanguage = state.selectedLanguage,
                onEvent = onEvent,
            )

            else -> LeetKeeperDashboard(
                state = state,
                onEvent = onEvent,
            )
        }
    }

    if (state.showCompletionSheet) {
        CompletionSheet(
            state = state,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun LeetKeeperDashboard(
    state: LeetKeeperUiState,
    onEvent: (LeetKeeperEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("leetkeeper_dashboard"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                LeetKeeperTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { onEvent(LeetKeeperEvent.SelectTab(tab)) },
                        text = { Text(tab.name) },
                        modifier = Modifier.testTag("leetkeeper_tab_${tab.name.lowercase()}"),
                    )
                }
            }
        }

        when (state.selectedTab) {
            LeetKeeperTab.Ongoing -> {
                if (state.ongoingPlan == null) {
                    item {
                        EmptyState(
                            title = "No active plan",
                            body = "Choose a popular plan when you are ready.",
                        )
                    }
                } else {
                    item {
                        OngoingPlanHeader(plan = state.ongoingPlan)
                    }
                    items(state.ongoingPlan.modules) { module ->
                        LeetKeeperModuleCard(
                            module = module,
                            onProblemClick = { onEvent(LeetKeeperEvent.OpenProblem(it)) },
                            onDoneClick = { onEvent(LeetKeeperEvent.RequestMarkDone(it)) },
                        )
                    }
                }
            }

            LeetKeeperTab.Popular -> {
                if (state.publicPlans.isEmpty() && !state.isLoading) {
                    item {
                        EmptyState(
                            title = "No plans yet",
                            body = "Plans from LeetKeeper will appear here.",
                        )
                    }
                } else {
                    items(state.publicPlans) { plan ->
                        PublicPlanCard(
                            plan = plan,
                            onClick = {
                                onEvent(LeetKeeperEvent.SelectPublicPlan(plan.id))
                            },
                        )
                    }
                }
            }

            LeetKeeperTab.Done -> item {
                EmptyState(
                    title = "Done plans",
                    body = "Completed LeetKeeper plans will appear here.",
                )
            }
        }

        if (state.errorMessage != null) {
            item {
                ErrorText(state.errorMessage)
            }
        }
    }
}

@Composable
private fun OngoingPlanHeader(plan: LeetKeeperOngoingPlan) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plan.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        plan.introduction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                LanguageChip(plan.language)
            }
            LinearProgressIndicator(
                progress = { progressFraction(plan.progress, plan.totalProblems) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .testTag("leetkeeper_plan_progress"),
            )
            Text(
                "${plan.progress}/${plan.totalProblems} problems",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PublicPlanCard(
    plan: LeetKeeperPublicPlan,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("leetkeeper_public_plan_${plan.id}"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                plan.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text("${plan.totalProblems} problems") },
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${plan.copy} active") },
                )
            }
            Text(
                plan.introduction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LeetKeeperModuleCard(
    module: LeetKeeperModule,
    onProblemClick: (LeetKeeperProblemSummary) -> Unit,
    onDoneClick: (LeetKeeperProblemSummary) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    module.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (module.progress == module.totalProblems && module.totalProblems > 0) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Module complete")
                } else {
                    CircularProgressIndicator(
                        progress = { progressFraction(module.progress, module.totalProblems) },
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${module.progress}/${module.totalProblems}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            module.problems.forEach { problem ->
                ProblemListItem(
                    problem = problem,
                    onProblemClick = onProblemClick,
                    onDoneClick = onDoneClick,
                )
            }
        }
    }
}

@Composable
private fun ProblemListItem(
    problem: LeetKeeperProblemSummary,
    onProblemClick: (LeetKeeperProblemSummary) -> Unit,
    onDoneClick: (LeetKeeperProblemSummary) -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                "${problem.id}. ${problem.title}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(problem.difficulty.label)
        },
        leadingContent = {
            Checkbox(
                checked = problem.done,
                enabled = !problem.done,
                onCheckedChange = {
                    if (it) {
                        onDoneClick(problem)
                    }
                },
                modifier = Modifier.testTag("leetkeeper_problem_done_${problem.id}"),
            )
        },
        trailingContent = {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Open problem")
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        ),
        modifier = Modifier
            .clickable { onProblemClick(problem) }
            .testTag("leetkeeper_problem_${problem.id}"),
    )
}

@Composable
private fun PublicPlanDetailPane(
    plan: LeetKeeperPublicPlan,
    selectedLanguage: LeetKeeperLanguage,
    onEvent: (LeetKeeperEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("leetkeeper_public_plan_detail"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onEvent(LeetKeeperEvent.ClosePublicPlanDetail) },
                    modifier = Modifier.testTag("leetkeeper_plan_detail_back"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    plan.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Text(
                plan.introduction,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            LanguageSelector(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = {
                    onEvent(LeetKeeperEvent.SelectLanguage(it))
                },
            )
        }
        item {
            Button(
                onClick = { onEvent(LeetKeeperEvent.StartSelectedPlan) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leetkeeper_start_plan_button"),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start")
            }
        }
        items(plan.modules) { module ->
            LeetKeeperModuleCard(
                module = module,
                onProblemClick = {},
                onDoneClick = {},
            )
        }
    }
}

@Composable
private fun LanguageSelector(
    selectedLanguage: LeetKeeperLanguage,
    onLanguageSelected: (LeetKeeperLanguage) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Language",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LeetKeeperLanguage.entries.forEach { language ->
                FilterChip(
                    selected = selectedLanguage == language,
                    onClick = { onLanguageSelected(language) },
                    label = { Text(language.label) },
                    leadingIcon = {
                        Image(
                            painter = painterResource(language.icon),
                            contentDescription = language.label,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ProblemDetailPane(
    problem: LeetKeeperProblemDetail,
    ongoingPlan: LeetKeeperOngoingPlan?,
    onEvent: (LeetKeeperEvent) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val isDone = ongoingPlan?.dones?.contains(problem.id.toIntOrNull()) == true

    Scaffold(
        modifier = Modifier.testTag("leetkeeper_problem_detail"),
        floatingActionButton = {
            if (ongoingPlan != null && !isDone) {
                FloatingActionButton(
                    onClick = {
                        onEvent(
                            LeetKeeperEvent.RequestMarkDone(
                                LeetKeeperProblemSummary(
                                    id = problem.id,
                                    title = problem.title,
                                    difficulty = problem.difficulty,
                                    done = false,
                                    link = problem.link,
                                )
                            )
                        )
                    },
                    modifier = Modifier.testTag("leetkeeper_mark_done_fab"),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Mark done")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onEvent(LeetKeeperEvent.CloseProblemDetail) },
                        modifier = Modifier.testTag("leetkeeper_problem_detail_back"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        "${problem.id}. ${problem.title}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    problemUrl(problem.link)?.let { url ->
                        IconButton(onClick = { uriHandler.openUri(url) }) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = "Open LeetCode")
                        }
                    }
                }
            }
            item {
                DifficultyChip(problem.difficulty)
            }
            item {
                Text(
                    text = AnnotatedString.fromHtml(problem.details),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("leetkeeper_problem_detail_body"),
                )
            }
            item {
                TransactionList(problem.transactions)
            }
            item {
                Spacer(Modifier.height(64.dp))
            }
        }
    }
}

@Composable
private fun TransactionList(transactions: List<LeetKeeperTransaction>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leetkeeper_transactions"),
    ) {
        Text(
            "Histories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (transactions.isEmpty()) {
            EmptyState(
                title = "No solved history",
                body = "New submissions will appear here.",
            )
        } else {
            transactions.forEach { transaction ->
                TransactionItem(transaction)
            }
        }
    }
}

@Composable
private fun TransactionItem(transaction: LeetKeeperTransaction) {
    val uriHandler = LocalUriHandler.current
    val submissionUrl = submissionUrl(transaction.problemLink, transaction.submission)
    ListItem(
        headlineContent = { Text(transaction.createdAt ?: "Unknown time") },
        supportingContent = { Text("${transaction.timeMinutes} minutes") },
        leadingContent = {
            Image(
                painter = painterResource(transaction.language.icon),
                contentDescription = transaction.language.label,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingContent = {
            if (submissionUrl != null) {
                Icon(Icons.Filled.OpenInNew, contentDescription = "Open submission")
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = submissionUrl != null) {
                submissionUrl?.let(uriHandler::openUri)
            },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompletionSheet(
    state: LeetKeeperUiState,
    onEvent: (LeetKeeperEvent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = { onEvent(LeetKeeperEvent.DismissCompletionSheet) },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("leetkeeper_completion_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                state.completionProblem?.title ?: "Mark done",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = state.ongoingPlan?.language?.label.orEmpty(),
                onValueChange = {},
                label = { Text("Language") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.durationText,
                onValueChange = { onEvent(LeetKeeperEvent.ChangeDuration(it)) },
                label = { Text("Time tracker (minutes)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leetkeeper_completion_duration"),
            )
            OutlinedTextField(
                value = state.submissionText,
                onValueChange = { onEvent(LeetKeeperEvent.ChangeSubmission(it)) },
                label = { Text("Submission id or link") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leetkeeper_completion_submission"),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { onEvent(LeetKeeperEvent.DismissCompletionSheet) }) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onEvent(LeetKeeperEvent.ConfirmCompletion) },
                    modifier = Modifier.testTag("leetkeeper_completion_confirm"),
                ) {
                    Text("Mark Done")
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(language: LeetKeeperLanguage) {
    AssistChip(
        onClick = {},
        label = { Text(language.label) },
        leadingIcon = {
            Image(
                painter = painterResource(language.icon),
                contentDescription = language.label,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

@Composable
private fun DifficultyChip(difficulty: LeetKeeperDifficulty) {
    val color = when (difficulty) {
        LeetKeeperDifficulty.Easy -> MaterialTheme.colorScheme.tertiaryContainer
        LeetKeeperDifficulty.Medium -> MaterialTheme.colorScheme.secondaryContainer
        LeetKeeperDifficulty.Hard -> MaterialTheme.colorScheme.errorContainer
    }
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            difficulty.label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun EmptyState(
    title: String,
    body: String,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Code, contentDescription = null)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ErrorText(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}

private fun progressFraction(progress: Int, total: Int): Float =
    if (total <= 0) 0f else progress.toFloat() / total.toFloat()

private fun problemUrl(link: String?): String? {
    val safeLink = link?.takeIf { it.isNotBlank() } ?: return null
    return if (safeLink.startsWith("http")) {
        safeLink
    } else {
        "https://www.leetcode.cn/problems/$safeLink"
    }
}

private fun submissionUrl(problemLink: String?, submission: String?): String? {
    val id = submission?.split("/")?.lastOrNull { it.all(Char::isDigit) } ?: return null
    val url = problemUrl(problemLink) ?: return null
    return "$url/submissions/$id"
}

@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun LeetKeeperScreenPreview() {
    TinkTheme(dynamicColor = false) {
        LeetKeeperScreen(
            state = sampleLeetKeeperState(),
        )
    }
}

internal fun sampleLeetKeeperState(): LeetKeeperUiState {
    val problems = listOf(
        LeetKeeperProblemSummary("1", "Two Sum", LeetKeeperDifficulty.Easy, true, "two-sum"),
        LeetKeeperProblemSummary("2", "Add Two Numbers", LeetKeeperDifficulty.Medium, false, "add-two-numbers"),
        LeetKeeperProblemSummary("3", "Longest Substring Without Repeating Characters", LeetKeeperDifficulty.Medium, false, "longest-substring-without-repeating-characters"),
    )
    val module = LeetKeeperModule(
        name = "Array and Hashing",
        totalProblems = problems.size,
        progress = 1,
        problems = problems,
    )
    val publicPlan = LeetKeeperPublicPlan(
        id = 1,
        title = "NeetCode 150",
        introduction = "A compact path through the most repeated interview patterns.",
        copy = 42,
        totalProblems = problems.size,
        modules = listOf(module),
    )
    return LeetKeeperUiState(
        selectedTab = LeetKeeperTab.Ongoing,
        isLoading = false,
        publicPlans = listOf(publicPlan),
        ongoingPlan = LeetKeeperOngoingPlan(
            id = "plan-1",
            title = publicPlan.title,
            introduction = publicPlan.introduction,
            language = LeetKeeperLanguage.Kotlin,
            totalProblems = publicPlan.totalProblems,
            progress = 1,
            dones = listOf(1),
            createdAt = "2026-06-01T00:00:00Z",
            updatedAt = "2026-06-21T00:00:00Z",
            modules = listOf(module),
        ),
        selectedPublicPlan = null,
        selectedLanguage = LeetKeeperLanguage.Kotlin,
        focusedProblem = null,
        completionProblem = null,
        durationText = "",
        submissionText = "",
        errorMessage = null,
    )
}
