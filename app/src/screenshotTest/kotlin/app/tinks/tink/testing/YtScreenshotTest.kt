package app.tinks.tink.testing

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.tinks.tink.ui.theme.TinkTheme
import app.tinks.tink.yt.*
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "compact-short", widthDp = 400, heightDp = 400)
@Preview(name = "compact-medium", widthDp = 400, heightDp = 500)
@Preview(name = "compact-tall", widthDp = 400, heightDp = 1000)
@Preview(name = "medium-short", widthDp = 610, heightDp = 400)
@Preview(name = "medium-medium", widthDp = 610, heightDp = 500)
@Preview(name = "medium-tall", widthDp = 610, heightDp = 1000)
@Preview(name = "expanded-short", widthDp = 900, heightDp = 400)
@Preview(name = "expanded-medium", widthDp = 900, heightDp = 500)
@Preview(name = "expanded-tall", widthDp = 900, heightDp = 1000)
@Preview(name = "appium", widthDp = 320, heightDp = 584)
@Preview(name = "large-font", widthDp = 400, heightDp = 500, fontScale = 1.5f)
@Composable
fun YtEmptyScreenshot() {
    TinkTheme(dynamicColor = false) { Surface { YtScreen(YtUiState()) } }
}

@PreviewTest
@Preview(widthDp = 400, heightDp = 740)
@Composable
fun YtDownloadingScreenshot() {
    TinkTheme(dynamicColor = false) { Surface {
        YtScreen(YtUiState(url = "https://youtu.be/LbCHAQ7HKu4", downloads = listOf(sampleYtDownload)))
    } }
}

@PreviewTest
@Preview(widthDp = 400, heightDp = 740)
@Composable
fun YtCompletedScreenshot() {
    TinkTheme(dynamicColor = false) { Surface {
        YtScreen(YtUiState(kind = YtDownloadKind.VIDEO, downloads = listOf(sampleYtDownload.copy(
            status = YtDownloadStatus.READY, kind = YtDownloadKind.VIDEO, sizeBytes = 24_000_000))))
    } }
}

@PreviewTest
@Preview(widthDp = 400, heightDp = 740)
@Composable
fun YtFailedScreenshot() {
    TinkTheme(dynamicColor = false) { Surface {
        YtScreen(YtUiState(downloads = listOf(sampleYtDownload.copy(status = YtDownloadStatus.FAILED,
            error = "Could not connect to YouTube. Check your network and tap Retry."))))
    } }
}

@PreviewTest
@Preview(widthDp = 400, heightDp = 500)
@Composable
fun YtDarkScreenshot() {
    TinkTheme(darkTheme = true, dynamicColor = false) { Surface { YtScreen(YtUiState()) } }
}

private val sampleYtDownload = YtDownload(
    id = 1, videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.AUDIO, attempt = "preview",
    title = "我又倒地了！影片深度分析 SGA", status = YtDownloadStatus.DOWNLOADING, progress = 42,
)
