package app.tinks.tink.book

import app.tinks.tink.network.ApiResult
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookRepositoryTest {

    @Test
    fun searchBooks_usesBackendSearchAndMapsResult() = runTest {
        val api = FakeBookApi()
        val repository = BookRepository(api)

        val emissions = repository.searchBooks("Le Guin").take(2).toList()

        assertTrue(emissions.first() is ApiResult.Loading)
        val books = (emissions.last() as ApiResult.Success).data
        assertEquals(listOf("Le Guin"), api.searchQueries)
        assertEquals("The Left Hand of Darkness", books.single().title)
        assertEquals("9780441478125", books.single().isbn)
    }

    @Test
    fun addToReading_createsWishlistEntryThenStartsReading() = runTest {
        val api = FakeBookApi()
        val repository = BookRepository(api)

        val result = repository.addToReading(BookDraft(title = "A Wizard of Earthsea"), "Paper")
            .take(2)
            .toList()
            .last() as ApiResult.Success

        assertEquals("A Wizard of Earthsea", api.createdDrafts.single().title)
        assertEquals("Paper", api.startedReading.single().second.platform)
        assertEquals(BookState.Reading, result.data.state)
    }

    @Test
    fun getBooks_passesSelectedCategoryToApi() = runTest {
        val api = FakeBookApi()
        val repository = BookRepository(api)

        repository.getBooks(BookState.Wish, "Science Fiction").take(2).toList()

        assertEquals(listOf("Science Fiction"), api.wishlistCategories)
    }

    private class FakeBookApi : BookApi {
        val searchQueries = mutableListOf<String>()
        val createdDrafts = mutableListOf<BookCreateRequest>()
        val startedReading = mutableListOf<Pair<Long, BookStartReadingRequest>>()
        val wishlistCategories = mutableListOf<String?>()

        override suspend fun search(keyword: String, limit: Int): List<BookSearchResultDto> {
            searchQueries.add(keyword)
            return listOf(
                BookSearchResultDto(
                    sourceId = "google-1",
                    title = "The Left Hand of Darkness",
                    author = "Ursula K. Le Guin",
                    isbn = "9780441478125",
                )
            )
        }

        override suspend fun createWishlistBook(payload: BookCreateRequest): BookDto {
            createdDrafts.add(payload)
            return bookDto(title = payload.title, state = "wish")
        }

        override suspend fun startReading(bookId: Long, payload: BookStartReadingRequest): BookDto {
            startedReading.add(bookId to payload)
            return bookDto(title = createdDrafts.single().title, state = "reading", platform = payload.platform)
        }

        override suspend fun getWishlist(page: Int, size: Int, category: String?): List<BookDto> {
            wishlistCategories.add(category)
            return emptyList()
        }
        override suspend fun getReading(page: Int, size: Int, category: String?): List<BookDto> = emptyList()
        override suspend fun getArchived(page: Int, size: Int, status: String?, category: String?): List<BookDto> = emptyList()
        override suspend fun getCategories(): List<String> = listOf("Science Fiction")
        override suspend fun getBook(bookId: Long): BookDto = bookDto()
        override suspend fun updateBook(bookId: Long, payload: BookUpdateRequest): BookDto = bookDto()
        override suspend fun deleteBook(bookId: Long) = Unit
        override suspend fun archiveBook(bookId: Long, payload: BookArchiveRequest): BookDto = bookDto(state = "archived")
        override suspend fun getNotes(bookId: Long, page: Int, size: Int): List<BookNoteDto> = emptyList()
        override suspend fun createNote(bookId: Long, payload: BookNoteCreateRequest): BookNoteDto =
            error("not used")
        override suspend fun updateNote(bookId: Long, noteId: Long, payload: BookNoteUpdateRequest): BookNoteDto =
            error("not used")
        override suspend fun deleteNote(bookId: Long, noteId: Long) = Unit
    }
}

private fun bookDto(
    title: String = "Test Book",
    state: String = "reading",
    platform: String? = null,
) = BookDto(
    id = 1,
    title = title,
    state = state,
    platform = platform,
    createdAt = "2026-05-25T08:00:00Z",
)
