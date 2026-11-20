package app.tinks.tink.connect

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
class ConnectScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun clearedHubDisablesDeleteAll() {
        render(ConnectUiState(loaded = true))
        compose.onNodeWithText("No files yet").assertExists()
        compose.onNodeWithTag("connect_delete_all").assertIsNotEnabled()
    }

    @Test fun deleteAllRequiresConfirmation() {
        var clears = 0
        render(ConnectUiState(hub = ConnectHub(text = "Shared text"), draft = "Shared text", loaded = true)) { clears++ }
        compose.onNodeWithTag("connect_delete_all").performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(0, clears)
        compose.onNodeWithTag("connect_delete_all").performClick()
        compose.onNodeWithTag("connect_confirm_clear").performClick()
        assertEquals(1, clears)
    }

    private fun render(state: ConnectUiState, onClear: () -> Unit = {}) {
        compose.setContent {
            TinkTheme(dynamicColor = false) {
                ConnectContent(
                    state = state,
                    onOpenDrawer = {},
                    onRefresh = {},
                    onUpload = {},
                    onRetry = {},
                    onDiscard = {},
                    onDownload = {},
                    onDelete = {},
                    onClear = onClear,
                    onEdit = {},
                    onPaste = {},
                    onCopy = {},
                    onShare = {},
                    onDismissMessage = {},
                )
            }
        }
    }
}
