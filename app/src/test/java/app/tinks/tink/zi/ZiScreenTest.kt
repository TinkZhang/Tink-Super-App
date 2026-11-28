package app.tinks.tink.zi

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import app.tinks.tink.ui.theme.TinkTheme
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class ZiScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cardsAndAddIconsHaveSeparateActions() {
        val actions = mutableListOf<String>()
        compose.setContent {
            TinkTheme(dynamicColor = false) {
                ZiDashboard(
                    uiState = ZiUiState(isLoading = false, showDialog = false, learntZiNum = 2),
                    written = WrittenZiListState(listOf("土"), isLoading = false),
                    onEvent = { if (it == ZiEvent.AddZiDialogOpen) actions += "add read" },
                    onReadList = { actions += "read list" },
                    onWrittenList = { actions += "written list" },
                    onAddWritten = { actions += "add written" },
                    onOpenTestGame = { actions += "open test game" },
                )
            }
        }
        compose.onNodeWithTag("zi_read_card").performClick()
        compose.onNodeWithTag("zi_add_read").performClick()
        compose.onNodeWithTag("zi_written_card").performClick()
        compose.onNodeWithTag("zi_add_written").performClick()
        compose.onNodeWithTag("zi_test_game_button").performClick()
        assertEquals(listOf("read list", "add read", "written list", "add written", "open test game"), actions)
    }

    @Test
    @Config(qualifiers = "w360dp-h760dp")
    fun dashboardKeepsFixedInsetsAndCentersReview() {
        compose.setContent {
            TinkTheme(dynamicColor = false) {
                ZiDashboard(
                    uiState = ZiUiState(
                        isLoading = false,
                        showDialog = false,
                        reviewList = listOf(Zi("土", LocalDate(2026, 1, 1), 2)),
                    ),
                    written = WrittenZiListState(emptyList(), isLoading = false),
                )
            }
        }

        val page = compose.onNodeWithTag("zi_dashboard").getUnclippedBoundsInRoot()
        val read = compose.onNodeWithTag("zi_read_card").getUnclippedBoundsInRoot()
        val written = compose.onNodeWithTag("zi_written_card").getUnclippedBoundsInRoot()
        val testButton = compose.onNodeWithTag("zi_test_game_button").getUnclippedBoundsInRoot()
        val review = compose.onNodeWithTag("zi_review_card").getUnclippedBoundsInRoot()
        val stories = compose.onNodeWithTag("zi_story_actions").getUnclippedBoundsInRoot()

        assertEquals(16f, (read.left - page.left).value, 1f)
        assertEquals(16f, (page.right - read.right).value, 1f)
        assertEquals(16f, (written.top - read.bottom).value, 1f)
        assertEquals(16f, (testButton.top - written.bottom).value, 1f)
        assertEquals(32f, (page.bottom - stories.bottom).value, 1f)
        assertEquals((review.top - testButton.bottom).value, (stories.top - review.bottom).value, 1f)
    }

    @Test fun candidateResultsDoNotMoveTheSelectedCharactersHeading() {
        val state = mutableStateOf(HandwritingState(model = InkModelState.Ready))
        compose.setContent {
            TinkTheme(dynamicColor = false) { ZiHandwritingScreen(state.value) }
        }
        val heading = compose.onNodeWithTag("zi_queued_title")
        val emptyTop = heading.getUnclippedBoundsInRoot().top

        compose.runOnIdle { state.value = state.value.copy(candidates = listOf("山", "木")) }

        assertEquals(emptyTop, heading.getUnclippedBoundsInRoot().top)
        compose.onNodeWithText("山").assertExists()
    }

    @Test fun boardQueuesCandidatesAndSupportsRemovingATile() {
        val state = mutableStateOf(HandwritingState(model = InkModelState.Ready, candidates = listOf("土", "士")))
        compose.setContent {
            TinkTheme(dynamicColor = false) {
                ZiHandwritingScreen(
                    state = state.value,
                    onSelect = { state.value = state.value.enqueue(it) },
                    onRemove = { state.value = state.value.remove(it) },
                )
            }
        }
        compose.onNodeWithTag("zi_confirm_written").assertIsNotEnabled()
        compose.onNodeWithText("士").performClick()
        compose.onNodeWithTag("zi_confirm_written").assertIsEnabled()
        compose.onNodeWithText("确认添加").assertExists()
        compose.onNodeWithText("土").assertDoesNotExist()
        compose.onNodeWithText("士").performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        compose.waitForIdle()
        compose.onNodeWithText("士").assertDoesNotExist()
        compose.onNodeWithTag("zi_confirm_written").assertIsNotEnabled()
    }
}
