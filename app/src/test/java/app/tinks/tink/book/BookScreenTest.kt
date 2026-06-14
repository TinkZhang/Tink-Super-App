package app.tinks.tink.book

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import app.tinks.tink.ui.theme.TinkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
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

        composeRule.onNodeWithTag("book_home_content").performScrollToIndex(2)
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
