package app.tinks.tink.home

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tinks.tink.book.BookEvent
import app.tinks.tink.book.BookPageFormat
import app.tinks.tink.book.BookState
import app.tinks.tink.book.sampleBook
import app.tinks.tink.ui.theme.TinkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readKeeperCell_usesOneFullWidthCardWithOnlyProgressAction() {
        val events = mutableListOf<HomeEvent>()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                HomeScreen(
                    state = HomeUiState(
                        snapshot = HomeSnapshot(readKeeperBook = readingBook()),
                    ),
                    onEvent = events::add,
                )
            }
        }

        composeRule.onNodeWithTag("home_readkeeper_cell").assertIsDisplayed()
        composeRule.onNodeWithText("Update progress").assertIsDisplayed().performClick()
        composeRule.onAllNodesWithText("Start").assertCountEquals(0)
        composeRule.onAllNodesWithText("Stop").assertCountEquals(0)
        assertEquals(HomeEvent.OpenReadKeeperProgress, events.single())
    }

    @Test
    fun homeProgressUsesTheReadKeeperDialogAndSubmitsTheOpenedBook() {
        val book = sampleBook(BookState.Reading)
        val events = mutableListOf<HomeEvent>()
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                HomeScreen(state = HomeUiState(readKeeperProgressBook = book), onEvent = events::add)
            }
        }

        composeRule.onNodeWithText("Update progress").assertIsDisplayed()
        composeRule.onNodeWithText("Log reading time").assertIsDisplayed()
        composeRule.onNodeWithTag("book_progress_save").performClick()

        assertEquals(
            HomeEvent.SaveReadKeeperProgress(BookEvent.UpdateReadingProgress(book.id, null, null, book.currentPage, null)),
            events.single(),
        )
    }

    private fun readingBook() = HomeReadKeeperBook(
        id = 1,
        title = "The Left Hand of Darkness",
        coverUrl = null,
        pageFormat = BookPageFormat.Page,
        currentPage = 120,
        progressPercentage = 33.3,
        pages = 360,
    )
}
