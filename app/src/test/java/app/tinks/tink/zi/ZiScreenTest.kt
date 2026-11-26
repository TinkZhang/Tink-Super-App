package app.tinks.tink.zi

import android.app.Application
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
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
                )
            }
        }
        compose.onNodeWithTag("zi_read_card").performClick()
        compose.onNodeWithTag("zi_add_read").performClick()
        compose.onNodeWithTag("zi_written_card").performClick()
        compose.onNodeWithTag("zi_add_written").performClick()
        assertEquals(listOf("read list", "add read", "written list", "add written"), actions)
    }

    @Test fun boardKeepsConfirmationDisabledUntilACharacterIsSelected() {
        var selected: String? = null
        compose.setContent {
            TinkTheme(dynamicColor = false) {
                ZiHandwritingScreen(
                    state = HandwritingState(model = InkModelState.Ready, candidates = listOf("土", "士")),
                    onSelect = { selected = it },
                )
            }
        }
        compose.onNodeWithTag("zi_confirm_written").assertIsNotEnabled()
        compose.onNodeWithText("士").performClick()
        assertEquals("士", selected)
    }
}
