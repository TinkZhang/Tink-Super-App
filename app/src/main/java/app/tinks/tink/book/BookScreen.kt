package app.tinks.tink.book

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Icon as AndroidIcon
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import app.tinks.tink.MainActivity
import app.tinks.tink.R
import app.tinks.tink.ui.components.YearContributionGraph
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.json.JSONObject

@Composable
fun BookScreen(
    viewModel: BookViewModel,
    onOpenDrawer: () -> Unit = {},
    stopReadingSessionRequestId: Int = 0,
    onStopReadingSessionRequestConsumed: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BookScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenDrawer = onOpenDrawer,
        stopReadingSessionRequestId = stopReadingSessionRequestId,
        onStopReadingSessionRequestConsumed = onStopReadingSessionRequestConsumed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookScreen(
    state: BookUiState,
    onEvent: (BookEvent) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    stopReadingSessionRequestId: Int = 0,
    onStopReadingSessionRequestConsumed: () -> Unit = {},
) {
    val navigationBackStack = state.navigationBackStack()
    val currentScreen = navigationBackStack.last()
    ReadingSessionNotificationEffect(state.readingSession)
    var showStopSession by remember { mutableStateOf(false) }
    var handledStopReadingSessionRequestId by remember { mutableIntStateOf(0) }
    val activeReadingSession = state.readingSession
    val activeReadingSessionBook = activeReadingSession?.let(state::findReadingSessionBook)
    LaunchedEffect(stopReadingSessionRequestId, activeReadingSession?.bookId, activeReadingSessionBook?.id) {
        if (
            stopReadingSessionRequestId > 0 &&
            stopReadingSessionRequestId != handledStopReadingSessionRequestId &&
            activeReadingSession != null &&
            activeReadingSessionBook != null
        ) {
            handledStopReadingSessionRequestId = stopReadingSessionRequestId
            showStopSession = true
            onStopReadingSessionRequestConsumed()
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            when (currentScreen) {
                BooksScreenState.Home -> HomeSearchBar(onEvent, onOpenDrawer)
                BooksScreenState.Search -> ActiveBookSearchBar(state, onEvent)
                is BooksScreenState.Detail -> Unit
                else -> BooksTopBar(
                    screen = currentScreen,
                    onOpenDrawer = onOpenDrawer,
                    onBack = { onEvent(BookEvent.NavigateBack) },
                    onSearch = { onEvent(BookEvent.OpenSearch) },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(Modifier.fillMaxSize()) {
                ReadKeeperNavDisplay(
                    backStack = navigationBackStack,
                    state = state,
                    onEvent = onEvent,
                    onRequestStopReadingSession = { showStopSession = true },
                    modifier = Modifier.fillMaxSize(),
                )
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
    if (showStopSession && activeReadingSession != null && activeReadingSessionBook != null) {
        StopReadingSessionDialog(
            session = activeReadingSession,
            book = activeReadingSessionBook,
            onDismiss = { showStopSession = false },
            onSave = { startTime, stopTime, page, progress ->
                onEvent(BookEvent.StopReadingSession(startTime, stopTime, page, progress))
                showStopSession = false
            },
        )
    }
}

private fun BookUiState.navigationBackStack(): List<BooksScreenState> =
    when {
        navigationStack.isEmpty() -> listOf(screen)
        navigationStack.last() == screen -> navigationStack
        else -> navigationStack + screen
    }

@Composable
private fun ReadKeeperNavDisplay(
    backStack: List<BooksScreenState>,
    state: BookUiState,
    onEvent: (BookEvent) -> Unit,
    onRequestStopReadingSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestState by rememberUpdatedState(state)
    val latestOnEvent by rememberUpdatedState(onEvent)
    val latestOnRequestStopReadingSession by rememberUpdatedState(onRequestStopReadingSession)
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { latestOnEvent(BookEvent.NavigateBack) },
    ) { screen ->
        NavEntry(screen) {
            ReadKeeperDestination(
                screen = screen,
                state = latestState,
                onEvent = latestOnEvent,
                onRequestStopReadingSession = latestOnRequestStopReadingSession,
            )
        }
    }
}

@Composable
private fun ReadKeeperDestination(
    screen: BooksScreenState,
    state: BookUiState,
    onEvent: (BookEvent) -> Unit,
    onRequestStopReadingSession: () -> Unit,
) {
    when (screen) {
        BooksScreenState.Home -> BooksHome(state, onEvent, onRequestStopReadingSession)
        is BooksScreenState.List -> BookList(
            state = screen.state,
            books = when (screen.state) {
                BookState.Reading -> state.readingBooks
                BookState.Wish -> state.wishlistBooks
                BookState.Archived -> state.archivedBooks.filterByStatus(state.selectedArchiveStatus)
            },
            categories = state.categories,
            selectedCategory = state.selectedCategory,
            selectedArchiveStatus = state.selectedArchiveStatus,
            emptyText = "No ${screen.state.label.lowercase()} books",
            onCategorySelected = { onEvent(BookEvent.SelectCategory(it)) },
            onArchiveStatusSelected = { onEvent(BookEvent.SelectArchiveStatus(it)) },
            onEvent = onEvent,
        )
        BooksScreenState.Search -> BookSearch(state, onEvent)
        is BooksScreenState.SearchResults -> BookSearch(state, onEvent)
        BooksScreenState.YearlySummary -> YearlySummaryScreen(state, onEvent)
        BooksScreenState.SummaryImage -> SummaryImageScreen(state, onEvent)
        is BooksScreenState.Detail -> BookDetail(
            book = state.selectedBook,
            notes = state.notes,
            categories = state.categories,
            readingSession = state.readingSession,
            onEvent = onEvent,
            onRequestStopReadingSession = onRequestStopReadingSession,
        )
        is BooksScreenState.Notes -> NotesList(state.notes, state.selectedBook, onEvent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BooksTopBar(
    screen: BooksScreenState,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
) {
    val title = when (screen) {
        BooksScreenState.Home -> "ReadKeeper"
        BooksScreenState.Search -> "Find a book"
        is BooksScreenState.SearchResults -> "Search results"
        is BooksScreenState.List -> screen.state.label
        BooksScreenState.YearlySummary -> "Reading summary"
        BooksScreenState.SummaryImage -> "Share image"
        is BooksScreenState.Detail -> "Book details"
        is BooksScreenState.Notes -> "Reading notes"
    }
    TopAppBar(
        title = { Text(title) },
        windowInsets = WindowInsets(0.dp),
        navigationIcon = {
            if (screen == BooksScreenState.Home) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("book_menu_button"),
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "打开抽屉")
                }
            } else {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("book_back_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上级")
                }
            }
        },
        actions = {
            if (screen is BooksScreenState.List || screen is BooksScreenState.SearchResults) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Search books")
                }
            }
        },
    )
}

@Composable
private fun BooksHome(
    state: BookUiState,
    onEvent: (BookEvent) -> Unit,
    onRequestStopReadingSession: () -> Unit,
) {
    val current = state.readingBooks.firstOrNull()
    val doneCount = state.archivedBooks.count { it.archiveStatus == ArchiveStatus.Done }
    val continueBooks = state.readingBooks.drop(if (current == null) 0 else 1).ifEmpty {
        current?.let(::listOf).orEmpty()
    }
    val activeReadingSession = state.readingSession?.takeIf { current != null && it.bookId == current.id }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("book_home_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (current == null) {
                item {
                    CurrentReadingCard(null, onEvent)
                }
            }
            if (current != null && activeReadingSession != null) {
                item {
                    ActiveReadingSessionCard(
                        session = activeReadingSession,
                        book = current,
                        onStop = onRequestStopReadingSession,
                    )
                }
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SummaryBadgeButton(
                        "Reading",
                        state.readingBooks.size,
                        Icons.Filled.Bookmark,
                        containerColor = ReadKeeperYellow,
                        contentColor = ReadKeeperOnYellow,
                        modifier = Modifier.weight(1f),
                    ) {
                        onEvent(BookEvent.OpenList(BookState.Reading))
                    }
                    SummaryBadgeButton(
                        "Wishlist",
                        state.wishlistBooks.size,
                        Icons.Filled.Favorite,
                        containerColor = ReadKeeperRed,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                    ) {
                        onEvent(BookEvent.OpenList(BookState.Wish))
                    }
                    SummaryBadgeButton(
                        "Archived",
                        state.archivedBooks.size,
                        Icons.Filled.Archive,
                        containerColor = ReadKeeperBlue,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                    ) {
                        onEvent(BookEvent.OpenList(BookState.Archived))
                    }
                    SummaryBadgeButton(
                        "Done",
                        doneCount,
                        Icons.Outlined.CheckCircle,
                        containerColor = ReadKeeperGreen,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                    ) {
                        onEvent(BookEvent.OpenArchivedStatus(ArchiveStatus.Done))
                    }
                }
            }
            if (continueBooks.isNotEmpty()) {
                item {
                    SectionHeader(title = "Continue reading", action = "All") {
                        onEvent(BookEvent.OpenList(BookState.Reading))
                    }
                }
                items(continueBooks.take(2), key = { it.id }) { book ->
                    BookListCard(book, onEvent)
                }
            }
            item {
                ReadingContributionCard(
                    year = Year.now().value,
                    markedDates = state.readingRecordDates,
                )
            }
            item {
                YearlySummaryEntry(state.currentYearlySummary()) {
                    onEvent(BookEvent.OpenYearlySummary)
                }
            }
        }

        current?.takeIf { activeReadingSession == null }?.let { book ->
            ReadingSessionFab(
                onStart = {
                    requestPostNotificationsPermissionIfNeeded(it)
                    onEvent(BookEvent.StartReadingSession(book))
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ReadingContributionCard(
    year: Int,
    markedDates: Set<LocalDate>,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("book_reading_contribution"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Reading records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "$year · ${markedDates.size.contributionDayText()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            YearContributionGraph(
                year = year,
                markedDates = markedDates,
                markedColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun Int.contributionDayText(): String =
    if (this == 1) "1 day" else "$this days"

private data class ReadingProgressLogDraft(
    val startTime: Instant,
    val stopTime: Instant,
    val startProgressLabel: String,
    val stopPage: Int?,
    val stopProgressPercentage: Double?,
)

@Composable
private fun ActiveReadingSessionCard(
    session: ReadingSessionState,
    book: Book,
    onStop: () -> Unit,
) {
    var now by remember(session.bookId, session.startTime) { mutableStateOf(Instant.now()) }
    LaunchedEffect(session.bookId, session.startTime) {
        while (true) {
            now = Instant.now()
            delay(1_000)
        }
    }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("book_active_reading_session"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Reading now",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = formatElapsedReadingDuration(session.startTime, now),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("book_reading_session_duration"),
                )
                Text(
                    text = "Started ${session.startTime.formatSessionTime()} · ${session.startProgressLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            FilledTonalIconButton(
                onClick = onStop,
                modifier = Modifier.testTag("book_active_reading_session_stop"),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = "Stop reading")
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action)
            }
        }
    }
}

@Composable
private fun YearlySummaryEntry(
    summary: YearlyReadingSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("book_yearly_summary"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.LibraryBooks,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Yearly ReadKeeper summary", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${summary.year} · ${summary.bookCount} books · ${summary.pageCount} pages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSearchBar(
    onEvent: (BookEvent) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = "",
                onQueryChange = {
                    onEvent(BookEvent.OpenSearch)
                    onEvent(BookEvent.SearchKeywordChanged(it))
                },
                onSearch = { onEvent(BookEvent.OpenSearch) },
                expanded = false,
                onExpandedChange = { if (it) onEvent(BookEvent.OpenSearch) },
                placeholder = { Text("Search books") },
                leadingIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("book_menu_button"),
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = "打开抽屉")
                    }
                },
            )
        },
        expanded = false,
        onExpandedChange = { if (it) onEvent(BookEvent.OpenSearch) },
        windowInsets = WindowInsets(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("book_search_entry"),
    ) {}
}

@Composable
private fun CurrentReadingCard(
    book: Book?,
    onEvent: (BookEvent) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("book_current_reading"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        if (book == null) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No active reading book", style = MaterialTheme.typography.titleLarge)
                Button(onClick = { onEvent(BookEvent.OpenSearch) }) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Find a book")
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(BookEvent.OpenDetail(book.id)) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BookCover(book.coverUrl, book.title, Modifier.width(88.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(book.author.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    ProgressLine(book)
                    FilledTonalButton(onClick = { onEvent(BookEvent.OpenNotes(book.id)) }) {
                        Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Notes")
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBadgeButton(
    label: String,
    count: Int,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier.height(88.dp),
        contentAlignment = Alignment.Center,
    ) {
        BadgedBox(
            badge = {
                Badge {
                    Text(count.badgeText())
                }
            },
        ) {
            FilledTonalIconButton(
                onClick = onClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                ),
                modifier = Modifier
                    .size(72.dp)
                    .testTag("book_list_${label.lowercase()}"),
            ) {
                Icon(
                    icon,
                    contentDescription = "$label ${count.badgeText()}",
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

private fun Int.badgeText(): String = if (this > 99) "99+" else toString()

@Composable
private fun BookList(
    state: BookState,
    books: List<Book>,
    categories: List<String>,
    selectedCategory: String?,
    selectedArchiveStatus: ArchiveStatus?,
    emptyText: String,
    onCategorySelected: (String?) -> Unit,
    onArchiveStatusSelected: (ArchiveStatus?) -> Unit,
    onEvent: (BookEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("book_list_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state == BookState.Archived) {
            item {
                ArchiveStatusFilters(
                    selectedStatus = selectedArchiveStatus,
                    onStatusSelected = onArchiveStatusSelected,
                )
            }
        }
        item {
            BookCategoryFilters(
                categories = categories,
                selectedCategory = selectedCategory,
                allLabel = if (state == BookState.Archived) "All categories" else "All",
                onCategorySelected = onCategorySelected,
            )
        }
        if (books.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        emptyText,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(books, key = { it.id }) { book ->
            BookListCard(book, onEvent)
        }
    }
}

@Composable
private fun BookCategoryFilters(
    categories: List<String>,
    selectedCategory: String?,
    allLabel: String = "All",
    onCategorySelected: (String?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text(allLabel) },
                modifier = Modifier.testTag("book_category_all"),
            )
        }
        items(categories, key = { it }) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                modifier = Modifier.testTag("book_category_$category"),
            )
        }
    }
}

@Composable
private fun ArchiveStatusFilters(
    selectedStatus: ArchiveStatus?,
    onStatusSelected: (ArchiveStatus?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onStatusSelected(null) },
                label = { Text("All") },
                modifier = Modifier.testTag("book_archive_status_all"),
            )
        }
        item {
            FilterChip(
                selected = selectedStatus == ArchiveStatus.Done,
                onClick = { onStatusSelected(ArchiveStatus.Done) },
                label = { Text("Done") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("book_archive_status_done"),
            )
        }
        item {
            FilterChip(
                selected = selectedStatus == ArchiveStatus.Abandon,
                onClick = { onStatusSelected(ArchiveStatus.Abandon) },
                label = { Text("Abandoned") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("book_archive_status_abandoned"),
            )
        }
    }
}

@Composable
internal fun BookListCard(
    book: Book,
    onEvent: (BookEvent) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEvent(BookEvent.OpenDetail(book.id)) }
            .testTag("book_item_${book.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BookCover(book.coverUrl, book.title, Modifier.width(88.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(154.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    book.author ?: book.publisher.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when (book.state) {
                    BookState.Reading -> ReadingListCardInfo(book)
                    BookState.Wish -> WishlistCardInfo(book, onEvent)
                    BookState.Archived -> ArchivedListCardInfo(book)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ReadingListCardInfo(book: Book) {
    BookMetadataRow(book.rating, book.pages, book.publishYear)
    book.platform?.takeIf { it.isNotBlank() }?.let { platform ->
        PlatformIconBadge(platform)
    }
    Spacer(Modifier.weight(1f))
    ProgressLine(book)
}

@Composable
private fun ColumnScope.WishlistCardInfo(
    book: Book,
    onEvent: (BookEvent) -> Unit,
) {
    BookMetadataRow(book.rating, book.pages, book.publishYear)
    Spacer(Modifier.weight(1f))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        book.category?.takeIf { it.isNotBlank() }?.let {
            AssistChip(onClick = {}, label = { Text(it) })
        } ?: Spacer(Modifier.width(1.dp))
        FilledTonalIconButton(
            onClick = { onEvent(BookEvent.MoveToReading(book)) },
            modifier = Modifier.testTag("book_move_to_reading_${book.id}"),
        ) {
            Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Move to reading")
        }
    }
}

@Composable
private fun ColumnScope.ArchivedListCardInfo(book: Book) {
    BookMetadataRow(book.rating, book.pages, book.publishYear)
    book.platform?.takeIf { it.isNotBlank() }?.let { platform ->
        PlatformIconBadge(platform)
    }
    Spacer(Modifier.weight(1f))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val status = book.archiveStatus
            Icon(
                archiveStatusIcon(status),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = archiveStatusColor(status),
            )
            Text(
                listOfNotNull(status?.label ?: "Archived", book.archivedDate).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = archiveStatusColor(status),
            )
        }
        Text(
            book.archiveProgressLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BookSearch(
    state: BookUiState,
    onEvent: (BookEvent) -> Unit,
) {
    val isResultPage = state.screen is BooksScreenState.SearchResults
    val context = LocalContext.current
    LaunchedEffect(state.searchErrorMessage) {
        state.searchErrorMessage?.takeIf { isResultPage }?.let { message ->
            Toast.makeText(context, "Book search failed: $message", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(state.toastId) {
        state.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when {
            isResultPage && state.searchResults.isNotEmpty() -> {
                items(state.searchResults) { draft ->
                    BookSearchResultCard(
                        draft = draft,
                        isWishlistChecked = draft.searchResultKey() in state.wishlistSearchResultKeys,
                        isReadingChecked = draft.searchResultKey() in state.readingSearchResultKeys,
                        onEvent = onEvent,
                    )
                }
            }
            isResultPage && state.searchErrorMessage != null -> {
                item {
                    SearchStateMessage(
                        title = "Search failed",
                        message = state.searchErrorMessage,
                    )
                }
            }
            isResultPage && !state.isLoading -> {
                item {
                    SearchStateMessage(
                        title = "No books found",
                        message = "Try a different title, author, or ISBN.",
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchStateMessage(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .testTag("book_search_state_message"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveBookSearchBar(
    state: BookUiState,
    onEvent: (BookEvent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = state.searchKeyword,
                onQueryChange = { onEvent(BookEvent.SearchKeywordChanged(it)) },
                onSearch = { onEvent(BookEvent.SubmitSearch) },
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .testTag("book_search_input"),
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text("Title, author, or ISBN") },
                leadingIcon = {
                    IconButton(
                        onClick = { onEvent(BookEvent.NavigateBack) },
                        modifier = Modifier.testTag("book_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上级")
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { onEvent(BookEvent.SubmitSearch) }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search books")
                    }
                },
            )
        },
        expanded = false,
        onExpandedChange = {},
        windowInsets = WindowInsets(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("book_search_field"),
    ) {}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BookSearchResultCard(
    draft: BookDraft,
    isWishlistChecked: Boolean = false,
    isReadingChecked: Boolean = false,
    onEvent: (BookEvent) -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { context.copyBookTitleAndOpenZlib(draft.title) },
            )
            .testTag("book_search_result_item"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BookCover(draft.coverUrl, draft.title, Modifier.width(88.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    draft.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    draft.author ?: draft.publisher.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BookMetadataRow(draft.rating, draft.pages, draft.publishYear)
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SearchResultToggleButton(
                        checked = isWishlistChecked,
                        checkedColor = ReadKeeperRed,
                        uncheckedColor = ReadKeeperRed,
                        checkedIcon = Icons.Filled.Favorite,
                        uncheckedIcon = Icons.Outlined.FavoriteBorder,
                        checkedContentDescription = "Added to wishlist",
                        uncheckedContentDescription = "Add to wishlist",
                        modifier = Modifier.testTag("book_add_wishlist"),
                    ) {
                        onEvent(
                            if (isWishlistChecked) {
                                BookEvent.UncheckDraftWishlist(draft)
                            } else {
                                BookEvent.AddDraftToWishlist(draft)
                            }
                        )
                    }
                    SearchResultToggleButton(
                        checked = isReadingChecked,
                        checkedColor = ReadKeeperYellow,
                        uncheckedColor = ReadKeeperYellow,
                        checkedIcon = Icons.Filled.Bookmark,
                        uncheckedIcon = Icons.Outlined.BookmarkAdd,
                        checkedContentDescription = "Added to reading",
                        uncheckedContentDescription = "Add to reading",
                        modifier = Modifier.testTag("book_add_reading"),
                    ) {
                        onEvent(
                            if (isReadingChecked) {
                                BookEvent.UncheckDraftReading(draft)
                            } else {
                                BookEvent.AddDraftToReading(draft)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun Context.copyBookTitleAndOpenZlib(title: String) {
    getSystemService(ClipboardManager::class.java)
        .setPrimaryClip(ClipData.newPlainText("Book title", title))

    val launchIntent = packageManager.getLaunchIntentForPackage(ZLIB_PACKAGE_NAME)
    if (launchIntent == null) {
        Toast.makeText(this, "Unable to open Z-Library app", Toast.LENGTH_SHORT).show()
        return
    }

    startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun SearchResultToggleButton(
    checked: Boolean,
    checkedColor: Color,
    uncheckedColor: Color,
    checkedIcon: ImageVector,
    uncheckedIcon: ImageVector,
    checkedContentDescription: String,
    uncheckedContentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (checked) checkedColor else uncheckedColor.copy(alpha = 0.14f),
            contentColor = if (checked) {
                if (checkedColor == ReadKeeperYellow) ReadKeeperOnYellow else Color.White
            } else {
                uncheckedColor
            },
        ),
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (checked) checkedIcon else uncheckedIcon,
            contentDescription = if (checked) checkedContentDescription else uncheckedContentDescription,
        )
    }
}

@Composable
private fun YearlySummaryScreen(
    state: BookUiState,
    onEvent: (BookEvent) -> Unit,
) {
    val years = state.summaryYears()
    val summary = state.currentYearlySummary()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("book_yearly_summary_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            YearSelector(
                year = summary.year,
                years = years,
                onYearSelected = { onEvent(BookEvent.SelectSummaryYear(it)) },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMetricCard("Books", summary.bookCount.toString(), Modifier.weight(1f))
                SummaryMetricCard("Pages", summary.pageCount.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onEvent(BookEvent.OpenSummaryImage) },
                    enabled = summary.books.isNotEmpty(),
                    modifier = Modifier.testTag("book_yearly_summary_save"),
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Create image")
                }
                FilledTonalButton(
                    onClick = { onEvent(BookEvent.OpenSummaryImage) },
                    enabled = summary.books.isNotEmpty(),
                    modifier = Modifier.testTag("book_yearly_summary_share"),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
        if (summary.books.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Text(
                        "No finished books for ${summary.year}.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        } else {
            item {
                Text("Book covers", style = MaterialTheme.typography.titleMedium)
            }
            items(summary.books.chunked(3)) { rowBooks ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowBooks.forEach { book ->
                        BookCover(
                            url = book.coverUrl,
                            title = book.title,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - rowBooks.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            item {
                SummaryBookList(summary.books)
            }
        }
    }
}

@Composable
private fun SummaryImageScreen(
    state: BookUiState,
    onEvent: (BookEvent) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val summary = state.currentYearlySummary()
    var posterBounds by remember { mutableStateOf<IntRect?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("book_summary_image_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            BookSummaryPoster(
                summary = summary,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    posterBounds = IntRect(
                        left = position.x.roundToInt(),
                        top = position.y.roundToInt(),
                        right = position.x.roundToInt() + coordinates.size.width,
                        bottom = position.y.roundToInt() + coordinates.size.height,
                    )
                },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val bitmap = captureBookSummaryBitmap(view, posterBounds)
                        if (bitmap == null) {
                            Toast.makeText(context, "Image is not ready yet", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            saveBookSummaryImage(context, bitmap, summary.year)
                        }
                    },
                    enabled = summary.books.isNotEmpty(),
                    modifier = Modifier.testTag("book_save_summary_image"),
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save image")
                }
                FilledTonalButton(
                    onClick = {
                        val bitmap = captureBookSummaryBitmap(view, posterBounds)
                        if (bitmap == null) {
                            Toast.makeText(context, "Image is not ready yet", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            shareBookSummaryImage(context, bitmap, summary.year)
                        }
                    },
                    enabled = summary.books.isNotEmpty(),
                    modifier = Modifier.testTag("book_share_summary_image"),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun YearSelector(
    year: Int,
    years: List<Int>,
    onYearSelected: (Int) -> Unit,
) {
    val index = years.indexOf(year).takeIf { it >= 0 } ?: 0
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { years.getOrNull(index + 1)?.let(onYearSelected) },
                enabled = years.getOrNull(index + 1) != null,
            ) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous year")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    year.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Reading summary", style = MaterialTheme.typography.labelLarge)
            }
            IconButton(
                onClick = { years.getOrNull(index - 1)?.let(onYearSelected) },
                enabled = years.getOrNull(index - 1) != null,
            ) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next year")
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BookSummaryPoster(
    summary: YearlyReadingSummary,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_readkeeperlogo),
                    contentDescription = "ReadKeeper",
                    modifier = Modifier.size(44.dp),
                    tint = Color.Unspecified,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "ReadKeeper",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        summary.year.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${summary.bookCount} books finished · ${summary.pageCount} pages",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            if (summary.books.isEmpty()) {
                Text("No finished books yet.", style = MaterialTheme.typography.bodyLarge)
            } else {
                SummaryPosterCoverGrid(summary.books)
                SummaryBookList(summary.books, containerColor = Color.Transparent)
            }
        }
    }
}

@Composable
private fun SummaryPosterCoverGrid(books: List<Book>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        books.take(12).chunked(4).forEach { rowBooks ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowBooks.forEach { book ->
                    BookCover(
                        url = book.coverUrl,
                        title = book.title,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - rowBooks.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryBookList(
    books: List<Book>,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Book list", style = MaterialTheme.typography.titleMedium)
            books.forEach { book ->
                SummaryBookRow(book)
            }
        }
    }
}

@Composable
private fun SummaryBookRow(book: Book) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                book.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${book.summaryStartDate()} - ${book.summaryEndDate()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            book.pages?.let { "$it pages" } ?: "-- pages",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        book.platform?.takeIf { it.isNotBlank() }?.let {
            PlatformIconBadge(it)
        }
    }
}

private data class YearlyReadingSummary(
    val year: Int,
    val books: List<Book>,
) {
    val bookCount: Int = books.size
    val pageCount: Int = books.sumOf { it.pages ?: it.currentPage ?: 0 }
}

private fun BookUiState.summaryYears(): List<Int> {
    val years = archivedBooks
        .filter { it.archiveStatus == ArchiveStatus.Done }
        .mapNotNull { it.summaryYear() }
        .distinct()
        .sortedDescending()
    return years.ifEmpty { listOf(Year.now().value) }
}

private fun BookUiState.currentYearlySummary(): YearlyReadingSummary {
    val years = summaryYears()
    val year = selectedSummaryYear?.takeIf { it in years } ?: years.first()
    val books = archivedBooks
        .filter { it.archiveStatus == ArchiveStatus.Done && it.summaryYear() == year }
        .sortedBy { it.archivedDate.orEmpty() }
    return YearlyReadingSummary(year = year, books = books)
}

private fun List<Book>.filterByStatus(status: ArchiveStatus?): List<Book> =
    status?.let { selected -> filter { it.archiveStatus == selected } } ?: this

private fun Book.summaryYear(): Int? = archivedDate?.take(4)?.toIntOrNull()

private fun Book.summaryStartDate(): String =
    createdAt?.take(10)?.takeIf { it.isNotBlank() } ?: "Start --"

private fun Book.summaryEndDate(): String = archivedDate?.takeIf { it.isNotBlank() } ?: "End --"

private fun Book.archiveProgressLabel(): String =
    category?.takeIf { it.isNotBlank() }
        ?: pageProgressLabel()
        ?: progressPercentageForDisplay()?.let { "${it.toInt()}%" }
        ?: ""

private fun archiveStatusIcon(status: ArchiveStatus?): ImageVector =
    if (status == ArchiveStatus.Done) Icons.Outlined.CheckCircle else Icons.Filled.Archive

@Composable
private fun archiveStatusColor(status: ArchiveStatus?): Color =
    if (status == ArchiveStatus.Abandon) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

private fun captureBookSummaryBitmap(
    view: android.view.View,
    bounds: IntRect?,
): Bitmap? {
    bounds ?: return null
    val root = view.drawToBitmap(Bitmap.Config.ARGB_8888)
    val left = bounds.left.coerceIn(0, root.width - 1)
    val top = bounds.top.coerceIn(0, root.height - 1)
    val right = bounds.right.coerceIn(left + 1, root.width)
    val bottom = bounds.bottom.coerceIn(top + 1, root.height)
    return Bitmap.createBitmap(root, left, top, right - left, bottom - top)
}

private fun saveBookSummaryImage(
    context: Context,
    bitmap: Bitmap,
    year: Int,
) {
    val name = "readkeeper-summary-$year-${System.currentTimeMillis()}.png"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ReadKeeper")
    }
    runCatching {
        val uri =
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create image")
        context.contentResolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: error("Unable to write image")
    }.onSuccess {
        Toast.makeText(context, "Saved to Photos", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "Unable to save image", Toast.LENGTH_SHORT).show()
    }
}

private fun shareBookSummaryImage(
    context: Context,
    bitmap: Bitmap,
    year: Int,
) {
    runCatching {
        val directory = File(context.cacheDir, "book-summary").apply { mkdirs() }
        val file = File(directory, "readkeeper-summary-$year.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share reading summary"))
    }.onFailure {
        Toast.makeText(context, "Unable to share image", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ReadingSessionNotificationEffect(session: ReadingSessionState?) {
    val context = LocalContext.current
    LaunchedEffect(session) {
        if (session == null) {
            context.cancelReadingSessionNotification()
        } else {
            context.showReadingSessionNotification(session)
        }
    }
}

private fun requestPostNotificationsPermissionIfNeeded(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        return
    }
    context.findActivity()?.let { activity ->
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            READKEEPER_NOTIFICATION_PERMISSION_REQUEST,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Context.showReadingSessionNotification(session: ReadingSessionState) {
    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel(
            READKEEPER_READING_SESSION_CHANNEL,
            "ReadKeeper reading",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Active ReadKeeper reading sessions"
        }
    )

    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val launchIntent = Intent(this, MainActivity::class.java).apply {
        action = MainActivity.ACTION_OPEN_READKEEPER_SESSION
    }
    val pendingIntent = PendingIntent.getActivity(
        this,
        READKEEPER_READING_SESSION_OPEN_REQUEST,
        launchIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val stopIntent = Intent(this, MainActivity::class.java).apply {
        action = MainActivity.ACTION_STOP_READKEEPER_SESSION
    }
    val stopPendingIntent = PendingIntent.getActivity(
        this,
        READKEEPER_READING_SESSION_STOP_REQUEST,
        stopIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val stopAction = Notification.Action.Builder(
        AndroidIcon.createWithResource(this, R.drawable.ic_qs_add_time),
        "Stop",
        stopPendingIntent,
    ).build()
    val notification = Notification.Builder(this, READKEEPER_READING_SESSION_CHANNEL)
        .setSmallIcon(R.drawable.ic_qs_add_time)
        .setContentTitle("Reading ${session.bookTitle}")
        .setContentText("Started at ${session.startTime.formatSessionTime()} · ${session.startProgressLabel}")
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setShowWhen(true)
        .setWhen(session.startTime.toEpochMilli())
        .setUsesChronometer(true)
        .addAction(stopAction)
        .setCategory(
            if (Build.VERSION.SDK_INT >= 36) Notification.CATEGORY_PROGRESS else Notification.CATEGORY_STATUS
        )
        .setProgress(0, 0, true)
        .apply {
            if (Build.VERSION.SDK_INT >= 36) {
                setStyle(
                    Notification.ProgressStyle()
                        .setProgressIndeterminate(true)
                        .setStyledByProgress(true)
                        .addProgressSegment(
                            Notification.ProgressStyle.Segment(100)
                                .setColor(AndroidColor.rgb(46, 125, 50))
                        )
                )
                setShortCriticalText("Reading")
            }
        }
        .build()
        .apply {
            addXiaomiIslandExtras(context = this@showReadingSessionNotification, session = session, stopAction = stopAction)
        }

    manager.notify(READKEEPER_READING_SESSION_NOTIFICATION_ID, notification)
}

private fun Context.cancelReadingSessionNotification() {
    getSystemService(NotificationManager::class.java)
        .cancel(READKEEPER_READING_SESSION_NOTIFICATION_ID)
}

private fun Notification.addXiaomiIslandExtras(
    context: Context,
    session: ReadingSessionState,
    stopAction: Notification.Action,
) {
    if (!context.supportsXiaomiIslandNotification()) return

    val actionKey = "miui.focus.action_readkeeper_stop"
    val iconKey = "miui.focus.pic_readkeeper"
    val actions = Bundle().apply {
        putParcelable(actionKey, stopAction)
    }
    val pics = Bundle().apply {
        putParcelable(iconKey, AndroidIcon.createWithResource(context, R.drawable.ic_readkeeperlogo))
    }
    extras.putBundle("miui.focus.actions", actions)
    extras.putBundle("miui.focus.pics", pics)
    extras.putString(
        "miui.focus.param",
        buildReadKeeperIslandParams(
            session = session,
            actionKey = actionKey,
            iconKey = iconKey,
        )
    )
}

private fun Context.supportsXiaomiIslandNotification(): Boolean =
    isXiaomiDevice() &&
        isSupportIsland("persist.sys.feature.island", false) &&
        focusProtocolVersion() >= XIAOMI_ISLAND_PROTOCOL_VERSION

private fun Context.isXiaomiDevice(): Boolean =
    Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
        Build.BRAND.equals("Xiaomi", ignoreCase = true) ||
        Build.BRAND.equals("Redmi", ignoreCase = true) ||
        Build.BRAND.equals("POCO", ignoreCase = true)

private fun isSupportIsland(key: String, defaultValue: Boolean): Boolean =
    runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getDeclaredMethod("getBoolean", String::class.java, java.lang.Boolean.TYPE)
        method.invoke(null, key, defaultValue) as? Boolean ?: defaultValue
    }.getOrDefault(defaultValue)

private fun Context.focusProtocolVersion(): Int =
    runCatching {
        Settings.System.getInt(contentResolver, "notification_focus_protocol", 0)
    }.getOrDefault(0)

private fun buildReadKeeperIslandParams(
    session: ReadingSessionState,
    actionKey: String,
    iconKey: String,
): String {
    val title = session.bookTitle.take(18)
    val content = "Started ${session.startTime.formatSessionTime()} · ${session.startProgressLabel}"
    return JSONObject().apply {
        put(
            "param_v2",
            JSONObject().apply {
                put("protocol", 1)
                put("business", "readkeeper_reading")
                put("enableFloat", false)
                put("updatable", true)
                put("filterWhenNoPermission", false)
                put("ticker", "ReadKeeper reading")
                put("tickerPic", iconKey)
                put("aodTitle", "Reading")
                put("aodPic", iconKey)
                put(
                    "param_island",
                    JSONObject().apply {
                        put("islandProperty", 2)
                        put("islandOrder", true)
                        put("islandTimeout", XIAOMI_ISLAND_TIMEOUT_SECONDS)
                        put("highlightColor", "#2E7D32")
                        put(
                            "bigIslandArea",
                            JSONObject().apply {
                                put(
                                    "imageTextInfoLeft",
                                    JSONObject().apply {
                                        put("type", 1)
                                        put(
                                            "picInfo",
                                            JSONObject().apply {
                                                put("type", 1)
                                                put("pic", iconKey)
                                            }
                                        )
                                        put(
                                            "miui.focus.paramtextInfo",
                                            JSONObject().apply {
                                                put("frontTitle", "阅读中")
                                                put("title", title)
                                                put("content", content)
                                                put("useHighLight", false)
                                            }
                                        )
                                    }
                                )
                            }
                        )
                        put(
                            "smallIslandArea",
                            JSONObject().apply {
                                put(
                                    "picInfo",
                                    JSONObject().apply {
                                        put("type", 1)
                                        put("pic", iconKey)
                                    }
                                )
                            }
                        )
                    }
                )
                put(
                    "baseInfo",
                    JSONObject().apply {
                        put("title", "Reading $title")
                        put("content", content)
                        put("type", 2)
                    }
                )
                put(
                    "hintInfo",
                    JSONObject().apply {
                        put("type", 1)
                        put("title", "Stop")
                        put(
                            "actionInfo",
                            JSONObject().apply {
                                put("action", actionKey)
                            }
                        )
                    }
                )
            }
        )
    }.toString()
}

@Composable
private fun BookMetadataRow(
    rating: Double?,
    pages: Int?,
    publishYear: Int?,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        rating?.let {
            SearchMetadataValue(
                icon = Icons.Filled.Star,
                text = String.format(Locale.US, "%.1f", it),
            )
        }
        pages?.let {
            SearchMetadataValue(
                icon = Icons.Outlined.AutoStories,
                text = "$it",
            )
        }
        publishYear?.let {
            SearchMetadataValue(
                icon = Icons.Outlined.CalendarMonth,
                text = "$it",
            )
        }
    }
}

@Composable
private fun SearchMetadataValue(
    icon: ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BookDetail(
    book: Book?,
    notes: List<BookNote>,
    categories: List<String>,
    readingSession: ReadingSessionState?,
    onEvent: (BookEvent) -> Unit,
    onRequestStopReadingSession: () -> Unit,
) {
    if (book == null) {
        BookDetailLoading(onEvent)
        return
    }
    var showEdit by remember(book.id) { mutableStateOf(false) }
    var showProgress by remember(book.id) { mutableStateOf(false) }
    var pendingProgressLog by remember(book.id) { mutableStateOf<ReadingProgressLogDraft?>(null) }
    var showDelete by remember(book.id) { mutableStateOf(false) }
    val activeReadingSession = readingSession?.takeIf { it.bookId == book.id }
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        book.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(BookEvent.NavigateBack) },
                        modifier = Modifier.testTag("book_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上级")
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit book")
                    }
                    if (book.state != BookState.Archived) {
                        IconButton(onClick = { onEvent(BookEvent.Archive(book.id, ArchiveStatus.Done)) }) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = "Mark done")
                        }
                        IconButton(onClick = { onEvent(BookEvent.Archive(book.id, ArchiveStatus.Abandon)) }) {
                            Icon(Icons.Filled.Archive, contentDescription = "Abandon")
                        }
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete book",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (book.state == BookState.Reading) {
                ExtendedFloatingActionButton(
                    onClick = { showProgress = true },
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    text = { Text("Progress") },
                    modifier = Modifier.testTag("book_edit_progress"),
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item { BookDetailHero(book) }
                if (book.state != BookState.Wish) {
                    item { ProgressLine(book) }
                }
                if (book.state == BookState.Reading && activeReadingSession != null) {
                    item {
                        ActiveReadingSessionCard(
                            session = activeReadingSession,
                            book = book,
                            onStop = onRequestStopReadingSession,
                        )
                    }
                }
                if (book.state == BookState.Wish) {
                    book.description?.takeIf { it.isNotBlank() }?.let { description ->
                        item {
                            DetailSectionTitle("Introduction")
                            Text(
                                description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    item {
                        DetailNotesSection(
                            notes = notes,
                            onOpenAllNotes = { onEvent(BookEvent.OpenNotes(book.id)) },
                        )
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
            if (book.state == BookState.Reading && activeReadingSession == null) {
                ReadingSessionFab(
                    onStart = {
                        requestPostNotificationsPermissionIfNeeded(it)
                        onEvent(BookEvent.StartReadingSession(book))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                )
            }
        }
    }

    if (showEdit) {
        EditBookDialog(
            book = book,
            categories = categories,
            onDismiss = { showEdit = false },
            onSave = { title, platform, category, pages, pageFormat ->
                onEvent(
                    BookEvent.SaveBook(
                        bookId = book.id,
                        title = title,
                        platform = platform,
                        category = category,
                        pages = pages,
                        pageFormat = pageFormat,
                        currentPage = null,
                        progressPercentage = null,
                    )
                )
                showEdit = false
            })
    }
    if (showProgress) {
        ProgressDialog(
            book = book,
            onDismiss = { showProgress = false },
            onSave = { page, progress ->
                pendingProgressLog = book.createProgressLogDraft(page = page, progress = progress)
                showProgress = false
            })
    }
    pendingProgressLog?.let { draft ->
        ReadingProgressTimeDialog(
            book = book,
            draft = draft,
            onDismiss = { pendingProgressLog = null },
            onSave = { startTime, stopTime, page, progress ->
                onEvent(
                    BookEvent.UpdateReadingProgress(
                        bookId = book.id,
                        startTime = startTime,
                        stopTime = stopTime,
                        stopPage = page,
                        stopProgressPercentage = progress,
                    )
                )
                pendingProgressLog = null
            },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            confirmButton = {
                TextButton(onClick = { onEvent(BookEvent.DeleteBook(book.id)) }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete book?") },
            text = { Text(book.title) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BookDetailLoading(onEvent: (BookEvent) -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("Book details") },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(BookEvent.NavigateBack) },
                        modifier = Modifier.testTag("book_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上级")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("book_detail_loading"),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ReadingSessionFab(
    onStart: (Context) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    ExtendedFloatingActionButton(
        onClick = { onStart(context) },
        icon = {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
            )
        },
        text = { Text("Start") },
        modifier = modifier.testTag("book_reading_session_fab"),
    )
}

@Composable
private fun StopReadingSessionDialog(
    session: ReadingSessionState,
    book: Book,
    onDismiss: () -> Unit,
    onSave: (Instant, Instant, Int?, Double?) -> Unit,
) {
    val stopTime = remember(session.bookId) { Instant.now() }
    ReadingTimeLogDialog(
        key = session.bookId,
        pageFormat = session.pageFormat,
        startTime = session.startTime,
        stopTime = stopTime,
        startProgressLabel = session.startProgressLabel,
        stopPage = book.currentPage,
        stopProgressPercentage = book.progressPercentage,
        onDismiss = onDismiss,
        onSave = onSave,
    )
}

@Composable
private fun ReadingProgressTimeDialog(
    book: Book,
    draft: ReadingProgressLogDraft,
    onDismiss: () -> Unit,
    onSave: (Instant, Instant, Int?, Double?) -> Unit,
) {
    ReadingTimeLogDialog(
        key = "${book.id}-${draft.stopPage}-${draft.stopProgressPercentage}-${draft.stopTime}",
        pageFormat = book.pageFormat,
        startTime = draft.startTime,
        stopTime = draft.stopTime,
        startProgressLabel = draft.startProgressLabel,
        stopPage = draft.stopPage,
        stopProgressPercentage = draft.stopProgressPercentage,
        onDismiss = onDismiss,
        onSave = onSave,
    )
}

@Composable
private fun ReadingTimeLogDialog(
    key: Any,
    pageFormat: BookPageFormat,
    startTime: Instant,
    stopTime: Instant,
    startProgressLabel: String,
    stopPage: Int?,
    stopProgressPercentage: Double?,
    onDismiss: () -> Unit,
    onSave: (Instant, Instant, Int?, Double?) -> Unit,
) {
    var startTimeText by remember(key, startTime) {
        mutableStateOf(startTime.formatSessionDateTimeInput())
    }
    var stopTimeText by remember(key, stopTime) {
        mutableStateOf(stopTime.formatSessionDateTimeInput())
    }
    var pageText by remember(key, stopPage) { mutableStateOf(stopPage?.toString().orEmpty()) }
    var progressText by remember(key, stopProgressPercentage) {
        mutableStateOf(stopProgressPercentage?.formatPercentInput(pageFormat).orEmpty())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        startTimeText.parseSessionDateTimeOrNull() ?: startTime,
                        stopTimeText.parseSessionDateTimeOrNull() ?: stopTime,
                        if (pageFormat.usesPages) pageText.toIntOrNull() else null,
                        if (pageFormat.usesPages) null else progressText.toDoubleOrNull()?.coerceIn(0.0, 100.0),
                    )
                },
                modifier = Modifier.testTag("book_stop_reading_session_save"),
            ) {
                Text("Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Reading session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startTimeText,
                    onValueChange = { startTimeText = it },
                    label = { Text("Start time") },
                    supportingText = { Text("yyyy-MM-dd HH:mm") },
                    singleLine = true,
                    modifier = Modifier.testTag("book_session_start_time"),
                )
                OutlinedTextField(
                    value = stopTimeText,
                    onValueChange = { stopTimeText = it },
                    label = { Text("Stop time") },
                    supportingText = { Text("yyyy-MM-dd HH:mm") },
                    singleLine = true,
                    modifier = Modifier.testTag("book_session_stop_time"),
                )
                BookDetailMetadata("From", startProgressLabel)
                if (pageFormat.usesPages) {
                    OutlinedTextField(
                        value = pageText,
                        onValueChange = { pageText = it.filter(Char::isDigit) },
                        label = { Text("Stop page") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.testTag("book_session_stop_page"),
                    )
                } else {
                    OutlinedTextField(
                        value = progressText,
                        onValueChange = { progressText = it.asPercentInput() },
                        label = { Text("Stop progress %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.testTag("book_session_stop_percent"),
                    )
                }
            }
        },
    )
}

@Composable
private fun BookDetailHero(book: Book) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        BookCover(book.coverUrl, book.title, Modifier.width(116.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BookDetailMetadata("Author", book.author)
            BookDetailMetadata("Press", book.publisher)
            BookDetailPlatformMetadata(book.platform)
            BookDetailMetadata("Pages", book.pages?.toString())
            BookDetailMetadata("Page type", if (book.pageFormat.usesPages) "Pages" else book.pageFormat.label)
            BookDetailMetadata("Publish year", book.publishYear?.toString())
            BookDetailMetadata("ISBN", book.isbn)
            BookDetailMetadata("Rating", book.rating?.let { String.format(Locale.US, "%.1f", it) })
        }
    }
}

@Composable
private fun BookDetailMetadata(
    label: String,
    value: String?,
) {
    value?.takeIf { it.isNotBlank() } ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(84.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BookDetailPlatformMetadata(platform: String?) {
    platform?.takeIf { it.isNotBlank() } ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            "Platform",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(84.dp),
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(platformIconDrawable(platform)),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.Unspecified,
            )
            Text(
                platform,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DetailSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun DetailNotesSection(
    notes: List<BookNote>,
    onOpenAllNotes: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailSectionTitle("Reading notes")
        if (notes.isEmpty()) {
            Text(
                "No notes yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            notes.take(3).forEach { note ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                note.content,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            val metadata = listOfNotNull(
                                note.page?.let { "Page $it" },
                                note.progressPercentage?.let { "${it.toInt()}%" },
                            ).joinToString(" · ")
                            if (metadata.isNotBlank()) {
                                Text(metadata)
                            }
                        },
                    )
                }
            }
        }
        FilledTonalButton(
            onClick = onOpenAllNotes,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("book_open_notes"),
        ) {
            Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("All notes")
        }
    }
}

@Composable
private fun BookMetadataSection(book: Book) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Book metadata",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                book.isbn?.takeIf { it.isNotBlank() }?.let { isbn ->
                    item { AssistChip(onClick = {}, label = { Text("ISBN $isbn") }) }
                }
                book.pages?.let { pages ->
                    item { AssistChip(onClick = {}, label = { Text("$pages pages") }) }
                }
                book.rating?.let { rating ->
                    item {
                        AssistChip(
                            onClick = {},
                            label = { Text(String.format(Locale.US, "%.1f rating", rating)) })
                    }
                }
                book.publisher?.takeIf { it.isNotBlank() }?.let { publisher ->
                    item { AssistChip(onClick = {}, label = { Text(publisher) }) }
                }
            }
        }
    }
}

@Composable
private fun NotesList(
    notes: List<BookNote>,
    book: Book?,
    onEvent: (BookEvent) -> Unit,
) {
    var editingNote by remember { mutableStateOf<BookNote?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("book_notes_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (notes.isEmpty()) {
                item { Text("No notes yet", style = MaterialTheme.typography.bodyLarge) }
            }
            items(notes, key = { it.id }) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingNote = note }
                ) {
                    ListItem(
                        headlineContent = { Text(note.content) },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                    note.page?.let { "Page $it" },
                                    note.progressPercentage?.let { "${it.toInt()}%" }).joinToString(
                                    " · "
                                )
                            )
                        },
                        trailingContent = {
                            IconButton(
                                onClick = { onEvent(BookEvent.DeleteNote(note.id)) },
                                modifier = Modifier.testTag("book_delete_note_${note.id}"),
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete note")
                            }
                        }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { showCreate = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("book_add_note"),
        ) {
            Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Add note")
        }
    }
    if (showCreate) {
        NoteDialog(book = book, note = null, onDismiss = { showCreate = false }, onSave = {
            onEvent(it)
            showCreate = false
        })
    }
    if (editingNote != null) {
        NoteDialog(book = book, note = editingNote, onDismiss = { editingNote = null }, onSave = {
            onEvent(it)
            editingNote = null
        })
    }
}

@Composable
private fun EditBookDialog(
    book: Book,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, Int?, BookPageFormat) -> Unit,
) {
    var title by remember(book.id) { mutableStateOf(book.title) }
    var platform by remember(book.id) { mutableStateOf(book.platform.orEmpty()) }
    var category by remember(book.id) { mutableStateOf(book.category.orEmpty()) }
    var pageText by remember(book.id) { mutableStateOf(book.pages?.toString().orEmpty()) }
    var pageFormat by remember(book.id) { mutableStateOf(book.pageFormat) }
    val platformOptions = listOf("Paper", "Kindle", "PDF", "WeRead")
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    title.trim(),
                    platform.trim().ifBlank { null },
                    category.trim().ifBlank { null },
                    pageText.toIntOrNull(),
                    pageFormat,
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Edit book") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") })
                Text("Platform", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(platformOptions, key = { it }) { option ->
                        FilterChip(
                            selected = platform == option,
                            onClick = { platform = option },
                            label = { Text(option) },
                            leadingIcon = {
                                Icon(
                                    platformIcon(option),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = platform,
                    onValueChange = { platform = it },
                    label = { Text("Custom platform") },
                    singleLine = true,
                )
                Text("Page type", style = MaterialTheme.typography.labelLarge)
                PageFormatSelector(pageFormat) { pageFormat = it }
                if (pageFormat.usesPages) {
                    OutlinedTextField(
                        value = pageText,
                        onValueChange = { pageText = it.filter(Char::isDigit) },
                        label = { Text("Pages") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.testTag("book_pages_input"),
                    )
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.testTag("book_category_input"),
                )
                if (categories.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it }) { value ->
                            FilterChip(
                                selected = category == value,
                                onClick = { category = value },
                                label = { Text(value) },
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun ProgressDialog(
    book: Book,
    onDismiss: () -> Unit,
    onSave: (Int?, Double?) -> Unit,
) {
    var pageText by remember(book.id) { mutableStateOf(book.currentPage?.toString().orEmpty()) }
    var progressText by remember(book.id) {
        mutableStateOf(book.progressPercentage?.formatPercentInput(book.pageFormat).orEmpty())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (book.pageFormat.usesPages) {
                        onSave(pageText.toIntOrNull(), null)
                    } else {
                        onSave(null, progressText.toDoubleOrNull()?.coerceIn(0.0, 100.0))
                    }
                },
                modifier = Modifier.testTag("book_progress_save"),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Update progress") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (book.pageFormat.usesPages) {
                    OutlinedTextField(
                        value = pageText,
                        onValueChange = { pageText = it.filter(Char::isDigit) },
                        label = { Text("Current page") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.testTag("book_progress_page"),
                    )
                } else {
                    OutlinedTextField(
                        value = progressText,
                        onValueChange = { progressText = it.asPercentInput() },
                        label = { Text("Progress %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.testTag("book_progress_percent"),
                    )
                }
            }
        },
    )
}

@Composable
private fun PageFormatSelector(
    selectedFormat: BookPageFormat,
    onFormatSelected: (BookPageFormat) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedFormat.usesPages,
                onClick = { onFormatSelected(BookPageFormat.Page) },
                label = { Text("Pages") },
            )
            FilterChip(
                selected = !selectedFormat.usesPages,
                onClick = {
                    onFormatSelected(
                        if (selectedFormat.usesPages) BookPageFormat.Percent100 else selectedFormat
                    )
                },
                label = { Text("Percent") },
            )
        }
        if (!selectedFormat.usesPages) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(BookPageFormat.Percent100, BookPageFormat.Percent1000, BookPageFormat.Percent10000),
                    key = { it.wireValue },
                ) { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { onFormatSelected(format) },
                        label = { Text(format.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteDialog(
    book: Book?,
    note: BookNote?,
    onDismiss: () -> Unit,
    onSave: (BookEvent.SaveNote) -> Unit,
) {
    val pageFormat = book?.pageFormat ?: BookPageFormat.Page
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var pageText by remember(note?.id) { mutableStateOf(note?.page?.toString().orEmpty()) }
    var progressText by remember(note?.id) {
        mutableStateOf(note?.progressPercentage?.formatPercentInput(pageFormat).orEmpty())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (content.isNotBlank()) {
                        onSave(
                            BookEvent.SaveNote(
                                noteId = note?.id,
                                content = content.trim(),
                                page = if (pageFormat.usesPages) pageText.toIntOrNull() else null,
                                progressPercentage = if (pageFormat.usesPages) {
                                    null
                                } else {
                                    progressText.toDoubleOrNull()?.coerceIn(0.0, 100.0)
                                },
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("book_save_note"),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(if (note == null) "Add note" else "Edit note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.testTag("book_note_content"),
                    label = { Text("Note") },
                )
                if (pageFormat.usesPages) {
                    OutlinedTextField(
                        value = pageText,
                        onValueChange = { pageText = it.filter(Char::isDigit) },
                        modifier = Modifier.testTag("book_note_page"),
                        label = { Text("Page") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                } else {
                    OutlinedTextField(
                        value = progressText,
                        onValueChange = { progressText = it.asPercentInput() },
                        label = { Text("Progress %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
            }
        },
    )
}

@Composable
private fun ProgressLine(book: Book) {
    val progress = (book.progressPercentageForDisplay() ?: 0.0).coerceIn(0.0, 100.0)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { (progress / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = if (book.pageFormat.usesPages) {
                listOfNotNull(
                    book.pageProgressLabel(),
                    "${progress.toInt()}%",
                ).joinToString(" · ")
            } else {
                progress.formatPercentLabel(book.pageFormat)
            },
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun Book.progressPercentageForDisplay(): Double? =
    if (pageFormat.usesPages) {
        currentPage?.let { page ->
            pages?.takeIf { it > 0 }?.let { totalPages ->
                page * 100.0 / totalPages
            }
        } ?: progressPercentage
    } else {
        progressPercentage
    }

private fun Book.pageProgressLabel(): String? =
    if (pageFormat.usesPages) {
        currentPage?.let { page -> pages?.let { "$page / $it pages" } ?: "Page $page" }
    } else {
        progressPercentage?.let { it.formatPercentLabel(pageFormat) }
    }

private fun Book.createProgressLogDraft(
    page: Int?,
    progress: Double?,
): ReadingProgressLogDraft {
    val stopTime = Instant.now()
    val estimatedPages = estimatedProgressDeltaPages(page = page, progress = progress)
    val minutes = maxOf(1L, ceil(estimatedPages / 2.0).toLong())
    return ReadingProgressLogDraft(
        startTime = stopTime.minusSeconds(minutes * 60),
        stopTime = stopTime,
        startProgressLabel = readingProgressPointLabel(currentPage, progressPercentage),
        stopPage = if (pageFormat.usesPages) page else null,
        stopProgressPercentage = if (pageFormat.usesPages) null else progress,
    )
}

private fun Book.estimatedProgressDeltaPages(
    page: Int?,
    progress: Double?,
): Double =
    if (pageFormat.usesPages) {
        ((page ?: currentPage ?: 0) - (currentPage ?: 0)).coerceAtLeast(0).toDouble()
    } else {
        val oldProgress = progressPercentage ?: 0.0
        val newProgress = progress ?: oldProgress
        val percentDelta = (newProgress - oldProgress).coerceAtLeast(0.0)
        pages?.takeIf { it > 0 }?.let { totalPages ->
            totalPages * percentDelta / 100.0
        } ?: percentDelta
    }

private fun Book.readingProgressPointLabel(
    page: Int?,
    progress: Double?,
): String =
    if (pageFormat.usesPages) {
        page?.let { "Page $it" } ?: "Page --"
    } else {
        progress?.formatPercentLabel(pageFormat) ?: "--%"
    }

private fun Double.formatPercentInput(format: BookPageFormat): String =
    if (format.precision == 0) {
        roundToInt().toString()
    } else {
        String.format(Locale.US, "%.${format.precision}f", this)
    }

private fun Double.formatPercentLabel(format: BookPageFormat): String =
    "${formatPercentInput(format)}%"

private fun String.asPercentInput(): String {
    var hasDecimal = false
    return buildString {
        this@asPercentInput.forEach { char ->
            when {
                char.isDigit() -> append(char)
                char == '.' && !hasDecimal -> {
                    append(char)
                    hasDecimal = true
                }
            }
        }
    }
}

private fun Instant.formatSessionTime(): String =
    atZone(ZoneId.systemDefault()).format(READING_SESSION_TIME_FORMATTER)

private fun Instant.formatSessionDateTimeInput(): String =
    atZone(ZoneId.systemDefault()).format(READING_SESSION_DATE_TIME_FORMATTER)

private fun String.parseSessionDateTimeOrNull(): Instant? =
    try {
        LocalDateTime.parse(trim(), READING_SESSION_DATE_TIME_FORMATTER)
            .atZone(ZoneId.systemDefault())
            .toInstant()
    } catch (_: DateTimeParseException) {
        null
    }

private fun formatElapsedReadingDuration(startTime: Instant, now: Instant): String {
    val duration = Duration.between(startTime, now).coerceAtLeast(Duration.ZERO)
    val hours = duration.toHours()
    val minutes = duration.toMinutesPart()
    val seconds = duration.toSecondsPart()
    return if (hours > 0) {
        "%d:%02d:%02d".format(Locale.US, hours, minutes, seconds)
    } else {
        "%02d:%02d".format(Locale.US, minutes, seconds)
    }
}

private fun BookUiState.findReadingSessionBook(session: ReadingSessionState): Book? =
    selectedBook?.takeIf { it.id == session.bookId }
        ?: readingBooks.firstOrNull { it.id == session.bookId }
        ?: wishlistBooks.firstOrNull { it.id == session.bookId }
        ?: archivedBooks.firstOrNull { it.id == session.bookId }

private const val READKEEPER_READING_SESSION_CHANNEL = "readkeeper_reading_session"
private const val READKEEPER_READING_SESSION_NOTIFICATION_ID = 4101
private const val READKEEPER_READING_SESSION_OPEN_REQUEST = 4103
private const val READKEEPER_READING_SESSION_STOP_REQUEST = 4104
private const val READKEEPER_NOTIFICATION_PERMISSION_REQUEST = 4102
private const val ZLIB_PACKAGE_NAME = "com.positron_it.zlib"
private const val XIAOMI_ISLAND_PROTOCOL_VERSION = 3
private const val XIAOMI_ISLAND_TIMEOUT_SECONDS = 60 * 60
private val ReadKeeperYellow = Color(0xFFF4B400)
private val ReadKeeperRed = Color(0xFFDB4437)
private val ReadKeeperBlue = Color(0xFF4285F4)
private val ReadKeeperGreen = Color(0xFF0F9D58)
private val ReadKeeperOnYellow = Color(0xFF1F1B16)
private val READING_SESSION_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm")
private val READING_SESSION_DATE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@Composable
private fun MetadataRow(book: Book) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { AssistChip(onClick = {}, label = { Text(book.state.label) }) }
        if (book.state == BookState.Reading || book.state == BookState.Archived) {
            book.platform?.takeIf { it.isNotBlank() }?.let { platform ->
                item { PlatformChip(platform) }
            }
        }
        if (book.publishYear != null) {
            item { AssistChip(onClick = {}, label = { Text(book.publishYear.toString()) }) }
        }
        if (!book.category.isNullOrBlank()) {
            item { AssistChip(onClick = {}, label = { Text(book.category) }) }
        }
    }
}

@Composable
private fun PlatformChip(
    platform: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            platformIcon(platform),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            platform,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun platformIcon(platform: String): ImageVector {
    val value = platform.lowercase(Locale.US)
    return when {
        "paper" in value || "book" in value -> Icons.Outlined.AutoStories
        "kindle" in value || "phone" in value || "app" in value -> Icons.Outlined.PhoneAndroid
        else -> Icons.Filled.Book
    }
}

@Composable
private fun PlatformIconBadge(
    platform: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(platformIconDrawable(platform)),
            contentDescription = platform,
            modifier = Modifier.size(18.dp),
            tint = Color.Unspecified,
        )
    }
}

private fun platformIconDrawable(platform: String): Int {
    val value = platform.lowercase(Locale.US)
    return when {
        "kindle" in value -> R.drawable.ic_kindle
        "pdf" in value -> R.drawable.ic_pdf
        "paper" in value -> R.drawable.ic_paper_book
        "apple" in value -> R.drawable.ic_apple_book
        "wechat" in value || "weread" in value || "we read" in value -> R.drawable.ic_wechat
        else -> R.drawable.ic_other_book
    }
}

@Composable
private fun BookCover(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.aspectRatio(0.68f),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (url.isNullOrBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Book, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        } else {
            AsyncImage(
                model = url,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp)),
            )
        }
    }
}

@Preview
@Composable
private fun BookScreenPreview() {
    BookScreen(
        state = BookUiState(
            readingBooks = listOf(
                Book(
                    id = 1,
                    title = "Designing Data-Intensive Applications",
                    publisher = null,
                    author = "Martin Kleppmann",
                    coverUrl = null,
                    isbn = null,
                    description = "A book about data systems.",
                    rating = null,
                    amazonLink = null,
                    pages = 616,
                    publishYear = 2017,
                    state = BookState.Reading,
                    platform = "Kindle",
                    pageFormat = BookPageFormat.Page,
                    currentPage = 120,
                    progressPercentage = 19.0,
                    archiveStatus = null,
                    archivedDate = null,
                )
            )
        )
    )
}
