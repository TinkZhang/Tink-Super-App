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

@PreviewTest
@Preview(name = "podcast-empty", widthDp = 400, heightDp = 740)
@Preview(name = "podcast-small", widthDp = 320, heightDp = 584)
@Composable
fun YtLibraryEmptyScreenshot() {
    TinkTheme(dynamicColor = false) { Surface { YtLibraryContent(YtLibraryState()) } }
}

@PreviewTest
@Preview(name = "podcast-playing", widthDp = 400, heightDp = 740)
@Preview(name = "podcast-large-text", widthDp = 400, heightDp = 740, fontScale = 1.5f)
@Composable
fun YtLibraryPlayingScreenshot() {
    val episode = sampleYtDownload.copy(status = YtDownloadStatus.READY, sizeBytes = 12_000_000)
    TinkTheme(dynamicColor = false) { Surface { YtLibraryContent(
        YtLibraryState(downloads = listOf(episode)),
        YtPlaybackState(true, episode.id, episode.title, true, 65_000, 900_000, 1.5f, true)) } }
}


@PreviewTest
@Preview(name = "subscription-listening-options", widthDp = 320, heightDp = 360)
@Preview(name = "subscription-listening-large-text", widthDp = 400, heightDp = 500, fontScale = 1.5f)
@Composable
fun YtSubscriptionListeningScreenshot() {
    TinkTheme(dynamicColor = false) { Surface {
        androidx.compose.foundation.layout.Column {
            SubscriptionListeningFields(true, {}, 1.5f, {})
        }
    } }
}

@PreviewTest
@Preview(widthDp = 400, heightDp = 740)
@Composable
fun YtLabelledLibraryScreenshot() {
    TinkTheme(dynamicColor = false) { Surface { YtLibraryContent(YtLibraryState(downloads = listOf(
        sampleYtDownload.copy(status = YtDownloadStatus.READY, standalone = true, labels = "History\nInterviews")))) } }
}

@PreviewTest
@Preview(name = "video-portrait", widthDp = 400, heightDp = 740)
@Preview(name = "video-landscape", widthDp = 740, heightDp = 400)
@Composable
fun YtVideoPlayerScreenshot() {
    TinkTheme(dynamicColor = false) { YtVideoContent(YtVideoState(title = "Downloaded video", available = true,
        loading = false, playing = true, position = 30_000, duration = 120_000)) }
}

@PreviewTest
@Preview(widthDp = 320, heightDp = 584)
@Composable
fun YtVideoMissingScreenshot() {
    TinkTheme(dynamicColor = false) { YtVideoContent(YtVideoState(loading = false, error = "This downloaded video is missing. Return to Downloads and retry it.")) }
}

@PreviewTest
@Preview(widthDp = 400, heightDp = 740)
@Composable
fun YtSubscriptionVideoLibraryScreenshot() {
    TinkTheme(dynamicColor = false) { Surface { YtLibraryContent(YtLibraryState(
        downloads = listOf(sampleYtDownload.copy(kind = YtDownloadKind.VIDEO, status = YtDownloadStatus.READY, standalone = false)),
        subscriptions = listOf(YtSubscription("p", "Video playlist", format = YtSubscriptionFormat.VIDEO)),
        entries = listOf(YtPlaylistEntry("p", sampleYtDownload.videoId, sampleYtDownload.title, 0, kind = YtDownloadKind.VIDEO)))) } }
}

@PreviewTest
@Preview(widthDp = 400, heightDp = 180)
@Composable
fun YtSubscriptionFormatsScreenshot() {
    TinkTheme(dynamicColor = false) { Surface { androidx.compose.foundation.layout.Column { SubscriptionFormatFields(YtSubscriptionFormat.VIDEO) {} } } }
}

@PreviewTest
@Preview(widthDp = 740, heightDp = 400)
@Composable
fun YtFullscreenVideoScreenshot() {
    TinkTheme(dynamicColor = false) { YtVideoContent(YtVideoState(title = "Video", loading = false, available = true), fullscreen = true) }
}

@PreviewTest
@Preview(widthDp = 400, heightDp = 140)
@Composable
fun YtOneTimeImportScreenshot() {
    TinkTheme(dynamicColor = false) { Surface { androidx.compose.foundation.layout.Column { SubscriptionImportMode(true) {} } } }
}

@PreviewTest
@Preview(widthDp = 400, heightDp = 740)
@Composable
fun YtSharedLinkDialogScreenshot() {
    TinkTheme(dynamicColor = false) { AddListeningDialog(initialUrl = "https://www.youtube.com/watch?v=LbCHAQ7HKu4", onDismiss = {}, onAdd = { _, _, _, _, _, _, _, _, _, _ -> }) }
}

@PreviewTest
@Preview(name = "library-dates-small", widthDp = 320, heightDp = 640)
@Preview(name = "library-dates-large-font", widthDp = 400, heightDp = 740, fontScale = 1.5f)
@Composable
fun YtLibraryDatesScreenshot() {
    TinkTheme(dynamicColor = false) { Surface { YtLibraryContent(YtLibraryState(
        downloads = listOf(sampleYtDownload.copy(status = YtDownloadStatus.READY, standalone = false, downloadedAt = 1791676800000)),
        subscriptions = listOf(YtSubscription("p", "A longer playlist title using the full width of the Library")),
        entries = listOf(YtPlaylistEntry("p", sampleYtDownload.videoId, sampleYtDownload.title, 0)))) } }
}
