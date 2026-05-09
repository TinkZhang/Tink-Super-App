package app.tinks.tink.book

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
fun BookScreen(viewModel: BookViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BookScreen(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookScreen(
    state: BookUiState,
    onEvent: (BookEvent) -> Unit = {},
) {
    Scaffold(
        floatingActionButton = {
            if (state.screen !is BooksScreenState.Search) {
                FloatingActionButton(onClick = { onEvent(BookEvent.OpenSearch) }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search books")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            BooksTabs(
                screen = state.screen,
                onEvent = onEvent,
            )
            Box(Modifier.fillMaxSize()) {
                when (val screen = state.screen) {
                    BooksScreenState.Home -> BooksHome(state, onEvent)
                    is BooksScreenState.List -> BookList(
                        books = when (screen.state) {
                            BookState.Reading -> state.readingBooks
                            BookState.Wish -> state.wishlistBooks
                            BookState.Archived -> state.archivedBooks
                        },
                        emptyText = "No ${screen.state.label.lowercase()} books",
                        onEvent = onEvent,
                    )
                    BooksScreenState.Search -> BookSearch(state, onEvent)
                    is BooksScreenState.Detail -> BookDetail(state.selectedBook, onEvent)
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

@Composable
private fun BooksTabs(
    screen: BooksScreenState,
    onEvent: (BookEvent) -> Unit,
) {
    val selectedIndex = when (screen) {
        BooksScreenState.Home -> 0
        is BooksScreenState.List -> when (screen.state) {
            BookState.Reading -> 1
            BookState.Wish -> 2
            BookState.Archived -> 3
        }
        BooksScreenState.Search -> 4
        else -> -1
    }
    SecondaryTabRow(selectedTabIndex = selectedIndex.coerceAtLeast(0)) {
        Tab(selected = selectedIndex == 0, onClick = { onEvent(BookEvent.OpenHome) }, text = { Text("Home") })
        Tab(selected = selectedIndex == 1, onClick = { onEvent(BookEvent.OpenList(BookState.Reading)) }, text = { Text("Reading") })
        Tab(selected = selectedIndex == 2, onClick = { onEvent(BookEvent.OpenList(BookState.Wish)) }, text = { Text("Wish") })
        Tab(selected = selectedIndex == 3, onClick = { onEvent(BookEvent.OpenList(BookState.Archived)) }, text = { Text("Archive") })
        Tab(selected = selectedIndex == 4, onClick = { onEvent(BookEvent.OpenSearch) }, text = { Text("Search") })
    }
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
            Text(
                "Books",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            CurrentReadingCard(current, onEvent)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Reading", state.readingBooks.size, Modifier.weight(1f)) {
                    onEvent(BookEvent.OpenList(BookState.Reading))
                }
                SummaryCard("Wishlist", state.wishlistBooks.size, Modifier.weight(1f)) {
                    onEvent(BookEvent.OpenList(BookState.Wish))
                }
                SummaryCard("Archived", state.archivedBooks.size, Modifier.weight(1f)) {
                    onEvent(BookEvent.OpenList(BookState.Archived))
                }
            }
        }
    }
}

@Composable
private fun CurrentReadingCard(
    book: Book?,
    onEvent: (BookEvent) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        Icon(Icons.Filled.NoteAdd, contentDescription = null)
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable { onClick() },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$count", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun BookList(
    books: List<Book>,
    emptyText: String,
    onEvent: (BookEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
private fun BookListCard(
    book: Book,
    onEvent: (BookEvent) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEvent(BookEvent.OpenDetail(book.id)) },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(book.coverUrl, book.title, Modifier.width(56.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(book.author.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                ProgressLine(book)
            }
            if (book.state == BookState.Wish) {
                IconButton(onClick = { onEvent(BookEvent.MoveToReading(book)) }) {
                    Icon(Icons.Filled.Visibility, contentDescription = "Move to reading")
                }
            }
        }
    }
}

@Composable
private fun BookSearch(
    state: BookUiState,
    onEvent: (BookEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.searchKeyword,
                    onValueChange = { onEvent(BookEvent.SearchKeywordChanged(it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Search Google Books") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
                Button(
                    onClick = { onEvent(BookEvent.SubmitSearch) },
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                }
            }
        }
        items(state.searchResults) { draft ->
            SearchResultCard(draft, onEvent)
        }
    }
}

@Composable
private fun SearchResultCard(
    draft: BookDraft,
    onEvent: (BookEvent) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BookCover(draft.coverUrl, draft.title, Modifier.width(64.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(draft.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(draft.author.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onEvent(BookEvent.AddDraftToWishlist(draft)) }) {
                        Text("Wishlist")
                    }
                    Button(onClick = { onEvent(BookEvent.AddDraftToReading(draft)) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Reading")
                    }
                }
            }
        }
    }
}

@Composable
private fun BookDetail(
    book: Book?,
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
                FilledTonalButton(onClick = { onEvent(BookEvent.OpenNotes(book.id)) }) {
                    Icon(Icons.Filled.NoteAdd, contentDescription = null)
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
        EditBookDialog(book = book, onDismiss = { showEdit = false }, onSave = { title, platform ->
            onEvent(
                BookEvent.SaveBook(
                    bookId = book.id,
                    title = title,
                    platform = platform,
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
            modifier = Modifier.fillMaxSize(),
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
                            IconButton(onClick = { onEvent(BookEvent.DeleteNote(note.id)) }) {
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
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.NoteAdd, contentDescription = "Add note")
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
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
) {
    var title by remember(book.id) { mutableStateOf(book.title) }
    var platform by remember(book.id) { mutableStateOf(book.platform.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(title.trim(), platform.trim().ifBlank { null }) }) {
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
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(if (note == null) "Add note" else "Edit note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Note") })
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it },
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = {}, label = { Text(book.state.label) })
        if (!book.platform.isNullOrBlank()) {
            AssistChip(onClick = {}, label = { Text(book.platform) })
        }
        if (book.publishYear != null) {
            AssistChip(onClick = {}, label = { Text(book.publishYear.toString()) })
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
