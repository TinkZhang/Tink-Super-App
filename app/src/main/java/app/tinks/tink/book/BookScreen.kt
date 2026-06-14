package app.tinks.tink.book

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import app.tinks.tink.R
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.time.Year
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BookScreen(
    viewModel: BookViewModel,
    onOpenDrawer: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BookScreen(state = state, onEvent = viewModel::onEvent, onOpenDrawer = onOpenDrawer)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookScreen(
    state: BookUiState,
    onEvent: (BookEvent) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
) {
    val navigationBackStack = state.navigationBackStack()
    val currentScreen = navigationBackStack.last()
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
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { onEvent(BookEvent.NavigateBack) },
    ) { screen ->
        NavEntry(screen) {
            ReadKeeperDestination(screen = screen, state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun ReadKeeperDestination(
    screen: BooksScreenState,
    state: BookUiState,
    onEvent: (BookEvent) -> Unit,
) {
    when (screen) {
        BooksScreenState.Home -> BooksHome(state, onEvent)
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
        BooksScreenState.YearlySummary -> YearlySummaryScreen(state, onEvent)
        BooksScreenState.SummaryImage -> SummaryImageScreen(state, onEvent)
        is BooksScreenState.Detail -> BookDetail(
            book = state.selectedBook,
            notes = state.notes,
            categories = state.categories,
            onEvent = onEvent,
        )
        is BooksScreenState.Notes -> NotesList(state.notes, onEvent)
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
            if (screen is BooksScreenState.List) {
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
) {
    val current = state.readingBooks.firstOrNull()
    val doneCount = state.archivedBooks.count { it.archiveStatus == ArchiveStatus.Done }
    val continueBooks = state.readingBooks.drop(if (current == null) 0 else 1).ifEmpty {
        current?.let(::listOf).orEmpty()
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("book_home_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CurrentReadingCard(current, onEvent)
        }
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                SummaryBadgeButton(
                    "Reading",
                    state.readingBooks.size,
                    Icons.Filled.Bookmark,
                ) {
                    onEvent(BookEvent.OpenList(BookState.Reading))
                }
                SummaryBadgeButton(
                    "Wishlist",
                    state.wishlistBooks.size,
                    Icons.Filled.Favorite,
                ) {
                    onEvent(BookEvent.OpenList(BookState.Wish))
                }
                SummaryBadgeButton(
                    "Archived",
                    state.archivedBooks.size,
                    Icons.Filled.Archive,
                ) {
                    onEvent(BookEvent.OpenList(BookState.Archived))
                }
                SummaryBadgeButton(
                    "Done",
                    doneCount,
                    Icons.Outlined.CheckCircle,
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
            YearlySummaryEntry(state.currentYearlySummary()) {
                onEvent(BookEvent.OpenYearlySummary)
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier.height(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .testTag("book_list_${label.lowercase()}"),
        ) {
            BadgedBox(
                badge = {
                    Badge {
                        Text(count.badgeText())
                    }
                },
            ) {
                Icon(
                    icon,
                    contentDescription = "$label ${count.badgeText()}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(state.searchResults) { draft ->
            BookSearchResultCard(draft, onEvent)
        }
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

@Composable
internal fun BookSearchResultCard(
    draft: BookDraft,
    onEvent: (BookEvent) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    IconButton(
                        onClick = { onEvent(BookEvent.AddDraftToWishlist(draft)) },
                        modifier = Modifier.testTag("book_add_wishlist"),
                    ) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Add to wishlist")
                    }
                    FilledTonalIconButton(
                        onClick = { onEvent(BookEvent.AddDraftToReading(draft)) },
                        modifier = Modifier.testTag("book_add_reading"),
                    ) {
                        Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Add to reading")
                    }
                }
            }
        }
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
    onEvent: (BookEvent) -> Unit,
) {
    if (book == null) return
    var showEdit by remember(book.id) { mutableStateOf(false) }
    var showProgress by remember(book.id) { mutableStateOf(false) }
    var showDelete by remember(book.id) { mutableStateOf(false) }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { BookDetailHero(book) }
            if (book.state != BookState.Wish) {
                item { ProgressLine(book) }
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
    }

    if (showEdit) {
        EditBookDialog(
            book = book,
            categories = categories,
            onDismiss = { showEdit = false },
            onSave = { title, platform, category ->
                onEvent(
                    BookEvent.SaveBook(
                        bookId = book.id,
                        title = title,
                        platform = platform,
                        category = category,
                        currentPage = book.currentPage,
                        progressPercentage = book.progressPercentage,
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
                onEvent(
                    BookEvent.SaveBook(
                        bookId = book.id,
                        title = book.title,
                        platform = book.platform,
                        category = book.category,
                        currentPage = page,
                        progressPercentage = progress,
                    )
                )
                showProgress = false
            })
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
        NoteDialog(note = null, onDismiss = { showCreate = false }, onSave = {
            onEvent(it)
            showCreate = false
        })
    }
    if (editingNote != null) {
        NoteDialog(note = editingNote, onDismiss = { editingNote = null }, onSave = {
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
    onSave: (String, String?, String?) -> Unit,
) {
    var title by remember(book.id) { mutableStateOf(book.title) }
    var platform by remember(book.id) { mutableStateOf(book.platform.orEmpty()) }
    var category by remember(book.id) { mutableStateOf(book.category.orEmpty()) }
    val platformOptions = listOf("Paper", "Kindle", "PDF", "WeRead")
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    title.trim(),
                    platform.trim().ifBlank { null },
                    category.trim().ifBlank { null },
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

private enum class ProgressInputMode {
    Page,
    Percent,
}

@Composable
private fun ProgressDialog(
    book: Book,
    onDismiss: () -> Unit,
    onSave: (Int?, Double?) -> Unit,
) {
    var pageText by remember(book.id) { mutableStateOf(book.currentPage?.toString().orEmpty()) }
    var progressText by remember(book.id) {
        mutableStateOf(
            book.progressPercentage?.toInt()?.toString().orEmpty()
        )
    }
    var inputMode by remember(book.id) {
        mutableStateOf(
            if (book.progressPercentage != null && book.currentPage == null) ProgressInputMode.Percent else ProgressInputMode.Page
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (inputMode == ProgressInputMode.Page) {
                    onSave(pageText.toIntOrNull(), null)
                } else {
                    onSave(null, progressText.toDoubleOrNull())
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Update progress") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProgressModeChips(inputMode) { inputMode = it }
                if (inputMode == ProgressInputMode.Page) {
                    OutlinedTextField(
                        value = pageText,
                        onValueChange = { pageText = it },
                        label = { Text("Current page") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                } else {
                    OutlinedTextField(
                        value = progressText,
                        onValueChange = { progressText = it },
                        label = { Text("Progress %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
            }
        },
    )
}

@Composable
private fun ProgressModeChips(
    inputMode: ProgressInputMode,
    onModeSelected: (ProgressInputMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = inputMode == ProgressInputMode.Page,
            onClick = { onModeSelected(ProgressInputMode.Page) },
            label = { Text("Page") },
        )
        FilterChip(
            selected = inputMode == ProgressInputMode.Percent,
            onClick = { onModeSelected(ProgressInputMode.Percent) },
            label = { Text("Percent") },
        )
    }
}

@Composable
private fun NoteDialog(
    note: BookNote?,
    onDismiss: () -> Unit,
    onSave: (BookEvent.SaveNote) -> Unit,
) {
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var pageText by remember(note?.id) { mutableStateOf(note?.page?.toString().orEmpty()) }
    var progressText by remember(note?.id) {
        mutableStateOf(
            note?.progressPercentage?.toInt()?.toString().orEmpty()
        )
    }
    var inputMode by remember(note?.id) {
        mutableStateOf(
            if (note?.let { it.progressPercentage != null && it.page == null } == true) {
                ProgressInputMode.Percent
            } else {
                ProgressInputMode.Page
            }
        )
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
                                page = if (inputMode == ProgressInputMode.Page) pageText.toIntOrNull() else null,
                                progressPercentage = if (inputMode == ProgressInputMode.Percent) progressText.toDoubleOrNull() else null,
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
                ProgressModeChips(inputMode) { inputMode = it }
                if (inputMode == ProgressInputMode.Page) {
                    OutlinedTextField(
                        value = pageText,
                        onValueChange = { pageText = it },
                        modifier = Modifier.testTag("book_note_page"),
                        label = { Text("Page") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                } else {
                    OutlinedTextField(
                        value = progressText,
                        onValueChange = { progressText = it },
                        label = { Text("Progress %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            text = listOfNotNull(
                book.pageProgressLabel(),
                "${progress.toInt()}%",
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun Book.progressPercentageForDisplay(): Double? =
    currentPage?.let { page ->
        pages?.takeIf { it > 0 }?.let { totalPages ->
            page * 100.0 / totalPages
        }
    } ?: progressPercentage

private fun Book.pageProgressLabel(): String? =
    currentPage?.let { page -> pages?.let { "$page / $it pages" } ?: "Page $page" }

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
                    currentPage = 120,
                    progressPercentage = 19.0,
                    archiveStatus = null,
                    archivedDate = null,
                )
            )
        )
    )
}
