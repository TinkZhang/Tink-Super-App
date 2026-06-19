package app.tinks.tink.book

import android.app.Application
import android.content.ClipboardManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.core.app.ApplicationProvider
import app.tinks.tink.ui.theme.TinkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Year

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class BookScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun home_opensReadingShelfFromIconSummary() {
        val events = mutableListOf<BookEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(readingBooks = listOf(sampleBook(BookState.Reading))),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithTag("book_list_reading").performClick()

        assertEquals(BookEvent.OpenList(BookState.Reading), events.single())
    }

    @Test
    fun home_startFabStartsLatestReadingBook() {
        val events = mutableListOf<BookEvent>()
        val latestBook = sampleBook(BookState.Reading)

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(readingBooks = listOf(latestBook)),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithTag("book_reading_session_fab").performClick()

        assertEquals(BookEvent.StartReadingSession(latestBook), events.single())
    }

    @Test
    fun home_updatesWhenReadingBooksArriveWithoutNavigation() {
        val latestBook = sampleBook(BookState.Reading)
        var state by mutableStateOf(BookUiState())

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(state = state)
            }
        }

        composeRule.onNodeWithText("No active reading book").assertIsDisplayed()

        composeRule.runOnIdle {
            state = state.copy(readingBooks = listOf(latestBook), isLoading = false)
        }

        composeRule.onAllNodesWithTag("book_current_reading").assertCountEquals(0)
        composeRule.onNodeWithText(latestBook.title).assertIsDisplayed()
    }

    @Test
    fun home_activeReadingSessionShowsTimerAndStopDialog() {
        val latestBook = sampleBook(BookState.Reading)
        val session = ReadingSessionState(
            bookId = latestBook.id,
            bookTitle = latestBook.title,
            pageFormat = latestBook.pageFormat,
            startTime = Instant.now().minusSeconds(65),
            startPage = latestBook.currentPage,
            startProgressPercentage = latestBook.progressPercentage,
            startProgressLabel = "Page 80",
        )

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        readingBooks = listOf(latestBook),
                        readingSession = session,
                    ),
                )
            }
        }

        composeRule.onAllNodesWithTag("book_current_reading").assertCountEquals(0)
        composeRule.onNodeWithTag("book_active_reading_session").assertIsDisplayed()
        composeRule.onNodeWithTag("book_reading_session_duration").assertIsDisplayed()
        composeRule.onNodeWithTag("book_active_reading_session_stop").performClick()
        composeRule.onNodeWithText("Reading session").assertIsDisplayed()
    }

    @Test
    fun home_showsReadingContributionGraph() {
        val year = Year.now().value

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        readingRecordDates = setOf(LocalDate.of(year, 1, 3)),
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("book_home_content").performScrollToIndex(2)
        composeRule.onNodeWithTag("book_reading_contribution").assertIsDisplayed()
        composeRule.onNodeWithText("Reading records").assertIsDisplayed()
        composeRule.onNodeWithText("$year · 1 day").assertIsDisplayed()
    }

    @Test
    fun notificationStopRequest_opensEditableStopDialogAndSavesEditedTimes() {
        val events = mutableListOf<BookEvent>()
        var consumed = false
        val latestBook = sampleBook(BookState.Reading)
        val session = ReadingSessionState(
            bookId = latestBook.id,
            bookTitle = latestBook.title,
            pageFormat = latestBook.pageFormat,
            startTime = Instant.parse("2026-06-17T11:00:00Z"),
            startPage = latestBook.currentPage,
            startProgressPercentage = latestBook.progressPercentage,
            startProgressLabel = "Page 80",
        )

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        readingBooks = listOf(latestBook),
                        readingSession = session,
                    ),
                    onEvent = events::add,
                    stopReadingSessionRequestId = 1,
                    onStopReadingSessionRequestConsumed = { consumed = true },
                )
            }
        }

        composeRule.onNodeWithText("Reading session").assertIsDisplayed()
        composeRule.onNodeWithTag("book_session_start_time").performTextClearance()
        composeRule.onNodeWithTag("book_session_start_time").performTextInput("2026-06-17 20:00")
        composeRule.onNodeWithTag("book_session_stop_time").performTextClearance()
        composeRule.onNodeWithTag("book_session_stop_time").performTextInput("2026-06-17 20:30")
        composeRule.onNodeWithTag("book_session_stop_page").performTextClearance()
        composeRule.onNodeWithTag("book_session_stop_page").performTextInput("96")
        composeRule.onNodeWithTag("book_stop_reading_session_save").performClick()

        assertEquals(true, consumed)
        assertEquals(
            BookEvent.StopReadingSession(
                startTime = LocalDateTime.parse("2026-06-17T20:00")
                    .atZone(ZoneId.systemDefault())
                    .toInstant(),
                stopTime = LocalDateTime.parse("2026-06-17T20:30")
                    .atZone(ZoneId.systemDefault())
                    .toInstant(),
                stopPage = 96,
                stopProgressPercentage = null,
            ),
            events.single(),
        )
    }

    @Test
    fun secondaryShelf_displaysBackArrowAndRequestsNavigationBack() {
        val events = mutableListOf<BookEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(screen = BooksScreenState.List(BookState.Wish)),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithText("Wishlist").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回上级").performClick()

        assertEquals(BookEvent.NavigateBack, events.single())
    }

    @Test
    fun shelf_categoryChipRequestsFiltering() {
        val events = mutableListOf<BookEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.List(BookState.Reading),
                        categories = listOf("Science Fiction"),
                    ),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithText("Science Fiction").performClick()

        assertEquals(BookEvent.SelectCategory("Science Fiction"), events.single())
    }

    @Test
    fun search_displaysBackendResults() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.SearchResults("left hand"),
                        searchKeyword = "left hand",
                        searchResults = listOf(
                            BookDraft(
                                title = "The Left Hand of Darkness",
                                author = "Ursula K. Le Guin",
                                isbn = "9780441478125",
                                pages = 304,
                                publishYear = 1969,
                            )
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("The Left Hand of Darkness").assertIsDisplayed()
        composeRule.onNodeWithText("Ursula K. Le Guin").assertIsDisplayed()
    }

    @Test
    fun searchResultButtons_emitToggleEventsForCheckedAndUncheckedStates() {
        val events = mutableListOf<BookEvent>()
        val draft = BookDraft(
            title = "The Left Hand of Darkness",
            author = "Ursula K. Le Guin",
            isbn = "9780441478125",
        )

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.SearchResults("left hand"),
                        searchResults = listOf(draft),
                        wishlistSearchResultKeys = setOf(draft.searchResultKey()),
                    ),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Added to wishlist").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add to reading").assertIsDisplayed()
        composeRule.onNodeWithTag("book_add_wishlist").performClick()
        composeRule.onNodeWithTag("book_add_reading").performClick()

        assertEquals(
            listOf(
                BookEvent.UncheckDraftWishlist(draft),
                BookEvent.AddDraftToReading(draft),
            ),
            events,
        )
    }

    @Test
    fun searchResultLongPress_copiesTitleToClipboard() {
        val draft = BookDraft(
            title = "The Left Hand of Darkness",
            author = "Ursula K. Le Guin",
            isbn = "9780441478125",
        )

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.SearchResults("left hand"),
                        searchResults = listOf(draft),
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("book_search_result_item").performTouchInput {
            longClick()
        }

        val clipboard = ApplicationProvider.getApplicationContext<Application>()
            .getSystemService(ClipboardManager::class.java)

        assertEquals(draft.title, clipboard.primaryClip?.getItemAt(0)?.text.toString())
    }

    @Test
    fun search_displaysEmptyResultMessage() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.SearchResults("unknown book"),
                        searchKeyword = "unknown book",
                        hasSubmittedSearch = true,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("book_search_state_message").assertIsDisplayed()
        composeRule.onNodeWithText("No books found").assertIsDisplayed()
    }

    @Test
    fun search_displaysErrorMessage() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.SearchResults("earthsea"),
                        searchKeyword = "earthsea",
                        hasSubmittedSearch = true,
                        searchErrorMessage = "Unexpected error occurred",
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("book_search_state_message").assertIsDisplayed()
        composeRule.onNodeWithText("Search failed").assertIsDisplayed()
    }

    @Test
    fun detailEdit_savesMetadataWithoutProgressFields() {
        val events = mutableListOf<BookEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.Detail(2),
                        selectedBook = sampleBook(BookState.Reading),
                    ),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Edit book").performClick()
        composeRule.onNodeWithText("Kindle").performClick()
        composeRule.onNodeWithText("Save").performClick()

        assertEquals(
            BookEvent.SaveBook(
                bookId = 2,
                title = "Currently reading",
                platform = "Kindle",
                category = null,
                pages = 200,
                pageFormat = BookPageFormat.Page,
                currentPage = null,
                progressPercentage = null,
            ),
            events.single(),
        )
    }

    @Test
    fun detailProgress_promptsForReadingTimeBeforeSaving() {
        val events = mutableListOf<BookEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.Detail(2),
                        selectedBook = sampleBook(BookState.Reading),
                    ),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithTag("book_edit_progress").performClick()
        composeRule.onNodeWithTag("book_progress_page").performTextClearance()
        composeRule.onNodeWithTag("book_progress_page").performTextInput("100")
        composeRule.onNodeWithTag("book_progress_save").performClick()
        composeRule.onNodeWithText("Reading session").assertIsDisplayed()
        composeRule.onNodeWithTag("book_session_start_time").assertIsDisplayed()
        composeRule.onNodeWithTag("book_stop_reading_session_save").performClick()

        val event = events.single() as BookEvent.UpdateReadingProgress
        assertEquals(2, event.bookId)
        assertEquals(100, event.stopPage)
        assertEquals(null, event.stopProgressPercentage)
    }

    @Test
    fun detail_activeReadingSessionShowsCardWithoutStopFab() {
        val book = sampleBook(BookState.Reading)
        val session = ReadingSessionState(
            bookId = book.id,
            bookTitle = book.title,
            pageFormat = book.pageFormat,
            startTime = Instant.now().minusSeconds(125),
            startPage = book.currentPage,
            startProgressPercentage = book.progressPercentage,
            startProgressLabel = "Page 80",
        )

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.Detail(book.id),
                        selectedBook = book,
                        readingSession = session,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("book_active_reading_session").assertIsDisplayed()
        composeRule.onAllNodesWithTag("book_reading_session_fab").assertCountEquals(0)
        composeRule.onAllNodesWithTag("book_active_reading_session_stop").assertCountEquals(1)
    }

    @Test
    fun detail_displaysLoadingShellBeforeBookArrives() {
        val events = mutableListOf<BookEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(screen = BooksScreenState.Detail(2), selectedBook = null),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithText("Book details").assertIsDisplayed()
        composeRule.onNodeWithTag("book_detail_loading").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回上级").performClick()

        assertEquals(BookEvent.NavigateBack, events.single())
    }

    @Test
    fun home_opensYearlyReadingSummary() {
        val events = mutableListOf<BookEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        archivedBooks = listOf(
                            sampleBook(BookState.Archived).copy(
                                archiveStatus = ArchiveStatus.Done,
                                archivedDate = "2026-02-01",
                            )
                        ),
                    ),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithTag("book_home_content").performScrollToIndex(3)
        composeRule.onNodeWithTag("book_yearly_summary").performClick()

        assertEquals(BookEvent.OpenYearlySummary, events.single())
    }

    @Test
    fun home_opensDoneArchiveListFromSummary() {
        val events = mutableListOf<BookEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        archivedBooks = listOf(
                            sampleBook(BookState.Archived).copy(
                                archiveStatus = ArchiveStatus.Done,
                                archivedDate = "2026-02-01",
                            )
                        ),
                    ),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithTag("book_list_done").performClick()

        assertEquals(BookEvent.OpenArchivedStatus(ArchiveStatus.Done), events.single())
    }

    @Test
    fun yearlySummary_showsTitleListWithoutAuthors() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.YearlySummary,
                        archivedBooks = listOf(
                            sampleBook(BookState.Archived).copy(
                                title = "Clean Code",
                                author = "Robert C. Martin",
                                archiveStatus = ArchiveStatus.Done,
                                archivedDate = "2026-03-14",
                            )
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("book_yearly_summary_content").performScrollToIndex(5)
        composeRule.onNodeWithText("Book list").assertIsDisplayed()
        composeRule.onNodeWithText("Clean Code").assertIsDisplayed()
        composeRule.onNodeWithText("200 pages").assertIsDisplayed()
        composeRule.onAllNodesWithText("Robert C. Martin").assertCountEquals(0)
    }

    @Test
    fun platformChip_isHiddenForWishlistBooks() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.List(BookState.Wish),
                        wishlistBooks = listOf(sampleBook(BookState.Wish).copy(platform = "Kindle")),
                    ),
                )
            }
        }

        composeRule.onAllNodesWithContentDescription("Kindle").assertCountEquals(0)
    }

    @Test
    fun platformChip_isShownForReadingBooks() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.List(BookState.Reading),
                        readingBooks = listOf(sampleBook(BookState.Reading).copy(platform = "Kindle")),
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Kindle").assertIsDisplayed()
    }

    @Test
    fun readingProgress_prefersCurrentPageOverStalePercentage() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.List(BookState.Reading),
                        readingBooks = listOf(
                            sampleBook(BookState.Reading).copy(
                                pages = 503,
                                currentPage = 204,
                                progressPercentage = 0.0,
                            )
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("204 / 503 pages · 40%").assertIsDisplayed()
    }

    @Test
    fun archivedList_statusChipRequestsFiltering() {
        val events = mutableListOf<BookEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                BookScreen(
                    state = BookUiState(
                        screen = BooksScreenState.List(BookState.Archived),
                        archivedBooks = listOf(
                            sampleBook(BookState.Archived).copy(
                                archiveStatus = ArchiveStatus.Done,
                                archivedDate = "2026-02-01",
                            )
                        ),
                    ),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithTag("book_archive_status_abandoned").performClick()

        assertEquals(BookEvent.SelectArchiveStatus(ArchiveStatus.Abandon), events.single())
    }
}
