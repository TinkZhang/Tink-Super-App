package app.tinks.tink

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import app.tinks.tink.book.BookScreen
import app.tinks.tink.haircut.HaircutScreen
import app.tinks.tink.lottery.LotteryHistoryStatsScreen
import app.tinks.tink.lottery.LotteryScreen
import app.tinks.tink.merriam.MerriamScreen
import app.tinks.tink.navigation.MyNavKey
import app.tinks.tink.navigation.ScreenA
import app.tinks.tink.navigation.ScreenB
import app.tinks.tink.navigation.ScreenBooks
import app.tinks.tink.navigation.ScreenHair
import app.tinks.tink.navigation.ScreenLearntZi
import app.tinks.tink.navigation.ScreenLeeter
import app.tinks.tink.navigation.ScreenLottery
import app.tinks.tink.navigation.ScreenLotteryHistoryStats
import app.tinks.tink.navigation.ScreenMerriam
import app.tinks.tink.navigation.ScreenSettings
import app.tinks.tink.navigation.ScreenStoryDetail
import app.tinks.tink.navigation.ScreenStoryList
import app.tinks.tink.navigation.ScreenTime
import app.tinks.tink.navigation.ScreenWeight
import app.tinks.tink.navigation.ScreenWeightHistory
import app.tinks.tink.navigation.ScreenZi
import app.tinks.tink.navigation.allTopDestinations
import app.tinks.tink.navigation.topDestination
import app.tinks.tink.settings.SettingsScreen
import app.tinks.tink.story.StoryDetailScreen
import app.tinks.tink.story.StoryListScreen
import app.tinks.tink.time.TimeEvent
import app.tinks.tink.time.TimeScreen
import app.tinks.tink.time.TimeViewModel
import app.tinks.tink.ui.components.AppSnackbarBus
import app.tinks.tink.weight.WeightHistoryScreen
import app.tinks.tink.weight.WeightScreen
import app.tinks.tink.zi.ZiScreen
import app.tinks.tink.zi.zilist.LearntZiListScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp(
    modifier: Modifier = Modifier,
    quickAddRequestId: Int = 0,
    openAddFromQuickSettings: Boolean = false,
    onQuickSettingsRequestConsumed: () -> Unit = {},
) {
    val backStack = remember { mutableStateListOf<MyNavKey>(ScreenA) }
    val currentKey = backStack.lastOrNull()
    val currentTopDestination = currentKey?.topDestination()
    var pendingQuickAddRequestId by remember { mutableStateOf<Int?>(null) }
    var handledQuickAddRequestId by remember { mutableStateOf<Int?>(null) }

    val onNavigate: (MyNavKey) -> Unit = { destination ->
        if (currentKey != destination) {
            if (backStack.isNotEmpty()) {
                backStack.removeLast()
            }
            backStack.add(destination)
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeLast()
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    val timeViewModel = hiltViewModel<TimeViewModel>()

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AppSnackbarBus.events.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.onAction()
            }
        }
    }

    LaunchedEffect(openAddFromQuickSettings, quickAddRequestId) {
        if (openAddFromQuickSettings) {
            onNavigate(ScreenTime)
            pendingQuickAddRequestId = quickAddRequestId
            onQuickSettingsRequestConsumed()
        }
    }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(Modifier.width(280.dp)) {
                Text(
                    "Tink's 2026",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(24.dp)
                )
                HorizontalDivider()
                allTopDestinations.forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.label) },
                        icon = { Icon(dest.icon, dest.label) },
                        selected = currentTopDestination == dest,
                        onClick = {
                            onNavigate(dest)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag(dest.drawerTestTag())
                    )
                }
            }
        },
        drawerState = drawerState,
        gesturesEnabled = true
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                if (currentKey !is ScreenBooks) {
                    TopAppBar(
                    title = { Text(currentKey?.label ?: "响应式应用") },
                    // 左上角添加菜单按钮，点击打开抽屉
                    navigationIcon = {
                        if (currentKey in allTopDestinations) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("top_bar_menu_button")
                            ) {
                                Icon(
                                    Icons.Filled.Menu,
                                    contentDescription = "打开抽屉"
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { navigateBack() },
                                modifier = Modifier.testTag("top_bar_back_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回上级"
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentKey == ScreenA || currentKey == ScreenTime) {
                            IconButton(
                                onClick = { timeViewModel.onEvent(TimeEvent.OpenLabelManager) },
                                modifier = Modifier.testTag("top_bar_time_labels_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Time labels")
                            }
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("设置") },
                                onClick = {
                                    menuExpanded = false
                                    backStack.add(ScreenSettings)
                                }
                            )
                        }
                    }
                    )
                }
            }
        ) { paddingValues ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                onBack = { navigateBack() },
            ) { key ->
                NavEntry(key) {
                    when (key) {
                        is ScreenA -> TimeScreen(timeViewModel)
                        is ScreenB -> WeightScreen(
                            hiltViewModel(),
                            onOpenHistory = { backStack.add(ScreenWeightHistory) },
                        )
                        is ScreenWeight -> WeightScreen(
                            hiltViewModel(),
                            onOpenHistory = { backStack.add(ScreenWeightHistory) },
                        )
                        is ScreenWeightHistory -> WeightHistoryScreen(hiltViewModel())
                        is ScreenHair -> HaircutScreen(hiltViewModel())
                        is ScreenLeeter -> HaircutScreen(hiltViewModel())
                        is ScreenZi -> ZiScreen(
                            hiltViewModel(),
                            onNavigationEvent = { backStack.add(ScreenLearntZi) },
                            onStoryListNavigation = { backStack.add(ScreenStoryList) }
                        )
                        is ScreenLearntZi -> LearntZiListScreen(hiltViewModel())
                        is ScreenMerriam -> MerriamScreen(hiltViewModel())
                        is ScreenTime -> {
                            LaunchedEffect(pendingQuickAddRequestId) {
                                val requestId = pendingQuickAddRequestId
                                if (requestId != null && requestId != handledQuickAddRequestId) {
                                    timeViewModel.onEvent(TimeEvent.OpenAddDialog)
                                    handledQuickAddRequestId = requestId
                                }
                            }
                            TimeScreen(timeViewModel)
                        }
                        is ScreenSettings -> SettingsScreen(hiltViewModel())
                        is ScreenBooks -> BookScreen(
                            hiltViewModel(),
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                        )
                        is ScreenLottery -> LotteryScreen(
                            hiltViewModel(),
                            onOpenHistoryStats = { backStack.add(ScreenLotteryHistoryStats) },
                        )
                        is ScreenLotteryHistoryStats -> LotteryHistoryStatsScreen(hiltViewModel())
                        is ScreenStoryList -> StoryListScreen(
                            hiltViewModel(),
                            onStoryClick = { story ->
                                backStack.add(ScreenStoryDetail(story.id))
                            }
                        )
                        is ScreenStoryDetail -> StoryDetailScreen(
                            hiltViewModel(),
                            storyId = key.storyId
                        )
                    }
                }
            }
        }
    }
}

private fun MyNavKey.drawerTestTag(): String = when (this) {
    ScreenA -> "drawer_destination_home"
    ScreenB, ScreenWeight -> "drawer_destination_weight"
    ScreenHair -> "drawer_destination_hair"
    ScreenLeeter -> "drawer_destination_leeter"
    ScreenZi -> "drawer_destination_zi"
    ScreenMerriam -> "drawer_destination_merriam"
    ScreenTime -> "drawer_destination_time"
    ScreenBooks -> "drawer_destination_books"
    ScreenLottery -> "drawer_destination_lottery"
    ScreenLotteryHistoryStats -> "drawer_destination_lottery_history_stats"
    ScreenSettings -> "drawer_destination_settings"
    ScreenLearntZi -> "drawer_destination_learnt_zi"
    ScreenStoryList -> "drawer_destination_story_list"
    is ScreenStoryDetail -> "drawer_destination_story_detail"
    ScreenWeightHistory -> "drawer_destination_weight_history"
}
