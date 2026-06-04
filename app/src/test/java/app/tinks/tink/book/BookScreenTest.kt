package app.tinks.tink.book

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
