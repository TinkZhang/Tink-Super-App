package app.tinks.tink.book

import app.tinks.tink.network.ApiResult
import app.tinks.tink.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BookViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsThreeShelves() = runTest {
        val viewModel = BookViewModel(FakeRepository())

        assertEquals("Currently reading", viewModel.uiState.value.readingBooks.single().title)
        assertEquals("Next book", viewModel.uiState.value.wishlistBooks.single().title)
        assertEquals("Finished book", viewModel.uiState.value.archivedBooks.single().title)
    }

    @Test
    fun listAndDetailNavigation_canReturnToPreviousScreens() = runTest {
        val viewModel = BookViewModel(FakeRepository())

        viewModel.onEvent(BookEvent.OpenList(BookState.Reading))
        viewModel.onEvent(BookEvent.OpenDetail(1))
        viewModel.onEvent(BookEvent.NavigateBack)
        assertEquals(BooksScreenState.List(BookState.Reading), viewModel.uiState.value.screen)

        viewModel.onEvent(BookEvent.NavigateBack)
        assertEquals(BooksScreenState.Home, viewModel.uiState.value.screen)
    }

    @Test
    fun selectingCategory_filtersTheOpenShelf() = runTest {
        val repository = FakeRepository()
        val viewModel = BookViewModel(repository)

        viewModel.onEvent(BookEvent.OpenList(BookState.Reading))
        viewModel.onEvent(BookEvent.SelectCategory("Science Fiction"))

        assertEquals("Science Fiction", viewModel.uiState.value.selectedCategory)
        assertEquals(BookState.Reading to "Science Fiction", repository.requests.last())
    }

    private class FakeRepository : BookRepository(NoopBookApi) {
        val requests = mutableListOf<Pair<BookState, String?>>()

        override fun getBooks(state: BookState, category: String?): Flow<ApiResult<List<Book>>> {
            requests.add(state to category)
            return flowOf(ApiResult.Success(listOf(sampleBook(state))))
        }

        override fun getCategories(): Flow<ApiResult<List<String>>> =
            flowOf(ApiResult.Success(listOf("Science Fiction")))

        override fun getBook(bookId: Long): Flow<ApiResult<Book>> =
            flowOf(ApiResult.Success(sampleBook(BookState.Reading)))
    }
}

private object NoopBookApi : BookApi {
    override suspend fun search(keyword: String, limit: Int): List<BookSearchResultDto> = emptyList()
    override suspend fun getWishlist(page: Int, size: Int, category: String?): List<BookDto> = emptyList()
    override suspend fun createWishlistBook(payload: BookCreateRequest): BookDto = error("not used")
    override suspend fun getReading(page: Int, size: Int, category: String?): List<BookDto> = emptyList()
    override suspend fun getArchived(page: Int, size: Int, status: String?, category: String?): List<BookDto> = emptyList()
    override suspend fun getCategories(): List<String> = emptyList()
    override suspend fun getBook(bookId: Long): BookDto = error("not used")
    override suspend fun updateBook(bookId: Long, payload: BookUpdateRequest): BookDto = error("not used")
    override suspend fun deleteBook(bookId: Long) = Unit
    override suspend fun startReading(bookId: Long, payload: BookStartReadingRequest): BookDto = error("not used")
    override suspend fun archiveBook(bookId: Long, payload: BookArchiveRequest): BookDto = error("not used")
    override suspend fun getNotes(bookId: Long, page: Int, size: Int): List<BookNoteDto> = emptyList()
    override suspend fun createNote(bookId: Long, payload: BookNoteCreateRequest): BookNoteDto = error("not used")
    override suspend fun updateNote(bookId: Long, noteId: Long, payload: BookNoteUpdateRequest): BookNoteDto =
        error("not used")
    override suspend fun deleteNote(bookId: Long, noteId: Long) = Unit
}

internal fun sampleBook(state: BookState) = Book(
    id = state.ordinal.toLong() + 1,
    title = when (state) {
        BookState.Reading -> "Currently reading"
        BookState.Wish -> "Next book"
        BookState.Archived -> "Finished book"
    },
    publisher = null,
    author = "Tink",
    coverUrl = null,
    isbn = null,
    description = null,
    rating = null,
    amazonLink = null,
    pages = 200,
    publishYear = 2026,
    state = state,
    platform = "Paper",
    currentPage = 80,
    progressPercentage = 40.0,
    archiveStatus = null,
    archivedDate = null,
)
