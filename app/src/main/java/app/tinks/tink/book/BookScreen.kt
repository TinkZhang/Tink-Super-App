package app.tinks.tink.book

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.util.Locale

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
    Scaffold(
        topBar = {
            when (state.screen) {
                BooksScreenState.Home -> HomeSearchBar(onEvent, onOpenDrawer)
                BooksScreenState.Search -> ActiveBookSearchBar(state, onEvent)
                else -> BooksTopBar(
                    screen = state.screen,
                    onOpenDrawer = onOpenDrawer,
                    onBack = { onEvent(BookEvent.NavigateBack) },
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
                when (val screen = state.screen) {
                    BooksScreenState.Home -> BooksHome(state, onEvent)
                    is BooksScreenState.List -> BookList(
                        books = when (screen.state) {
                            BookState.Reading -> state.readingBooks
                            BookState.Wish -> state.wishlistBooks
                            BookState.Archived -> state.archivedBooks
                        },
                        categories = state.categories,
                        selectedCategory = state.selectedCategory,
                        emptyText = "No ${screen.state.label.lowercase()} books",
                        onCategorySelected = { onEvent(BookEvent.SelectCategory(it)) },
                        onEvent = onEvent,
                    )
                    BooksScreenState.Search -> BookSearch(state, onEvent)
                    is BooksScreenState.Detail -> BookDetail(state.selectedBook, state.categories, onEvent)
                    is BooksScreenState.Notes -> NotesList(state.notes, onEvent)
                }
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BooksTopBar(
    screen: BooksScreenState,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
) {
    val title = when (screen) {
        BooksScreenState.Home -> "Reading tracker"
        BooksScreenState.Search -> "Find a book"
        is BooksScreenState.List -> screen.state.label
        is BooksScreenState.Detail -> "Book details"
        is BooksScreenState.Notes -> "Reading notes"
    }
    TopAppBar(
        title = { Text(title) },
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
    )
}

@Composable
private fun BooksHome(
    state: BookUiState,
    onEvent: (BookEvent) -> Unit,
) {
    val current = state.readingBooks.firstOrNull()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CurrentReadingCard(current, onEvent)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Reading", state.readingBooks.size, Icons.Filled.Bookmark, Modifier.weight(1f)) {
                    onEvent(BookEvent.OpenList(BookState.Reading))
                }
                SummaryCard("Wishlist", state.wishlistBooks.size, Icons.Filled.Favorite, Modifier.weight(1f)) {
                    onEvent(BookEvent.OpenList(BookState.Wish))
                }
                SummaryCard("Archived", state.archivedBooks.size, Icons.Filled.Archive, Modifier.weight(1f)) {
                    onEvent(BookEvent.OpenList(BookState.Archived))
                }
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
                    Text(book.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
private fun SummaryCard(
    label: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("book_list_${label.lowercase()}"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            }
            Text("$count", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BookList(
    books: List<Book>,
    categories: List<String>,
    selectedCategory: String?,
    emptyText: String,
    onCategorySelected: (String?) -> Unit,
    onEvent: (BookEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("book_list_content"),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            BookCategoryFilters(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected,
            )
        }
        if (books.isEmpty()) {
            item {
                Text(emptyText, style = MaterialTheme.typography.bodyLarge)
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
    onCategorySelected: (String?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") },
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
                    .height(130.dp),
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
        SearchMetadataValue(icon = Icons.Outlined.PhoneAndroid, text = platform)
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
    FilledTonalIconButton(
        onClick = { onEvent(BookEvent.MoveToReading(book)) },
        modifier = Modifier
            .align(Alignment.End)
            .testTag("book_move_to_reading_${book.id}"),
    ) {
        Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Move to reading")
    }
}

@Composable
private fun ColumnScope.ArchivedListCardInfo(book: Book) {
    BookMetadataRow(book.rating, book.pages, book.publishYear)
    Spacer(Modifier.weight(1f))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            book.archiveStatus?.label ?: "Archived",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        book.archivedDate?.takeIf { it.isNotBlank() }?.let { date ->
            Text(
                "· $date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (book.currentPage != null || book.progressPercentage != null) {
        ProgressLine(book)
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

@Composable
private fun BookDetail(
    book: Book?,
    categories: List<String>,
    onEvent: (BookEvent) -> Unit,
) {
    if (book == null) return
    var showEdit by remember(book.id) { mutableStateOf(false) }
    var showProgress by remember(book.id) { mutableStateOf(false) }
    var showDelete by remember(book.id) { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BookCover(book.coverUrl, book.title, Modifier.width(112.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(book.author.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    MetadataRow(book)
                    ProgressLine(book)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showProgress = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Progress")
                }
                FilledTonalButton(
                    onClick = { onEvent(BookEvent.OpenNotes(book.id)) },
                    modifier = Modifier.testTag("book_open_notes"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Notes")
                }
                OutlinedButton(onClick = { showEdit = true }) {
                    Text("Edit")
                }
            }
        }
        item {
            if (!book.description.isNullOrBlank()) {
                Text(book.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (book.state != BookState.Archived) {
                    OutlinedButton(onClick = { onEvent(BookEvent.Archive(book.id)) }) {
                        Icon(Icons.Filled.Archive, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Archive")
                    }
                }
                OutlinedButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
    }

    if (showEdit) {
        EditBookDialog(book = book, categories = categories, onDismiss = { showEdit = false }, onSave = { title, platform, category ->
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
        ProgressDialog(book = book, onDismiss = { showProgress = false }, onSave = { page, progress ->
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
                            Text(listOfNotNull(note.page?.let { "Page $it" }, note.progressPercentage?.let { "${it.toInt()}%" }).joinToString(" · "))
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
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = platform, onValueChange = { platform = it }, label = { Text("Platform") })
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
    var progressText by remember(book.id) { mutableStateOf(book.progressPercentage?.toInt()?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(pageText.toIntOrNull(), progressText.toDoubleOrNull())
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Update progress") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it },
                    label = { Text("Current page") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = progressText,
                    onValueChange = { progressText = it },
                    label = { Text("Progress %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
    )
}

@Composable
private fun NoteDialog(
    note: BookNote?,
    onDismiss: () -> Unit,
    onSave: (BookEvent.SaveNote) -> Unit,
) {
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var pageText by remember(note?.id) { mutableStateOf(note?.page?.toString().orEmpty()) }
    var progressText by remember(note?.id) { mutableStateOf(note?.progressPercentage?.toInt()?.toString().orEmpty()) }
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
                                page = pageText.toIntOrNull(),
                                progressPercentage = progressText.toDoubleOrNull(),
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
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it },
                    modifier = Modifier.testTag("book_note_page"),
                    label = { Text("Page") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = progressText,
                    onValueChange = { progressText = it },
                    label = { Text("Progress %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
    )
}

@Composable
private fun ProgressLine(book: Book) {
    val progress = ((book.progressPercentage ?: book.currentPage?.let { page ->
        book.pages?.takeIf { it > 0 }?.let { page * 100.0 / it }
    }) ?: 0.0).coerceIn(0.0, 100.0)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { (progress / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = listOfNotNull(
                book.currentPage?.let { page -> book.pages?.let { "$page / $it pages" } ?: "Page $page" },
                "${progress.toInt()}%",
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun MetadataRow(book: Book) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { AssistChip(onClick = {}, label = { Text(book.state.label) }) }
        if (!book.platform.isNullOrBlank()) {
            item { AssistChip(onClick = {}, label = { Text(book.platform) }) }
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
