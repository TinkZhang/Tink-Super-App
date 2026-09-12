package app.tinks.tink.yt

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.tinks.tink.ui.theme.TinkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class YtScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyScreenDisablesDownloadAndEmitsInputAndKindChanges() {
        var url = ""
        var kind = YtDownloadKind.AUDIO
        compose.setContent { TinkTheme(dynamicColor = false) {
            YtScreen(YtUiState(), onUrlChange = { url = it }, onKindChange = { kind = it })
        } }
        compose.onNodeWithTag("yt_empty").assertIsDisplayed()
        compose.onNodeWithTag("yt_download_button").assertIsNotEnabled()
        compose.onNodeWithTag("yt_url_input").performTextInput("https://youtu.be/LbCHAQ7HKu4")
        compose.onNodeWithTag("yt_kind_video").performClick()
        assertEquals("https://youtu.be/LbCHAQ7HKu4", url)
        assertEquals(YtDownloadKind.VIDEO, kind)
    }

    @Test fun failedDownloadOffersRetryAndDisplaysError() {
        var retry = 0
        val item = YtDownload(id = 1, videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.AUDIO,
            attempt = "one", status = YtDownloadStatus.FAILED, error = "Check your network.")
        compose.setContent { TinkTheme(dynamicColor = false) {
            YtScreen(YtUiState(downloads = listOf(item)), onRetry = { retry = it })
        } }
        compose.onNodeWithTag("yt_retry_1").performScrollTo().performClick()
        compose.onNodeWithText("Check your network.").assertIsDisplayed()
        assertEquals(1, retry)
    }

    @Test fun completedDownloadOpensTheMatchingLocalItem() {
        var opened: YtDownload? = null
        val item = YtDownload(id = 2, videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.VIDEO,
            attempt = "one", status = YtDownloadStatus.READY, title = "Saved video", sizeBytes = 1_500_000)
        compose.setContent { TinkTheme(dynamicColor = false) {
            YtScreen(YtUiState(downloads = listOf(item)), onOpen = { opened = it })
        } }
        compose.onNodeWithTag("yt_open_2").performScrollTo().performClick()
        assertEquals(item, opened)
        compose.onNodeWithTag("yt_retry_2").assertDoesNotExist()
    }

    @Test fun activeDownloadOffersCancelAndSubmissionShowsError() {
        var cancelled = 0
        val item = YtDownload(id = 3, videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.AUDIO,
            attempt = "one", status = YtDownloadStatus.DOWNLOADING, progress = 35)
        compose.setContent { TinkTheme(dynamicColor = false) {
            YtScreen(YtUiState(downloads = listOf(item), error = "Enter a valid YouTube video link."), onCancel = { cancelled = it })
        } }
        compose.onNodeWithTag("yt_error").assertIsDisplayed()
        compose.onNodeWithTag("yt_cancel_3").performScrollTo().performClick()
        assertEquals(3, cancelled)
    }
}
