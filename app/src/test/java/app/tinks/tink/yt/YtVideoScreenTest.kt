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

// Authored only: UI tests require the user's explicit request.
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class YtVideoScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun videoControlsDispatchPauseSpeedAndBack() {
        var toggled = false
        var back = false
        var speed = 1f
        compose.setContent { TinkTheme { YtVideoContent(YtVideoState(playing = true, available = true, loading = false, duration = 100_000),
            onToggle = { toggled = true }, onBack = { back = true }, onSpeed = { speed = it }) } }
        compose.onNodeWithTag("yt_video_toggle").performClick()
        assertTrue(toggled)
        compose.onNodeWithTag("yt_video_speed").performClick()
        compose.onNodeWithText("1.5×").performClick()
        assertEquals(1.5f, speed)
        compose.onNodeWithTag("yt_video_back").performClick()
        assertTrue(back)
    }
    @Test fun missingFileShowsErrorAndDisablesPlayback() {
        compose.setContent { TinkTheme { YtVideoContent(YtVideoState(loading = false, error = "Video is missing")) } }
        compose.onNodeWithTag("yt_video_error").assertTextEquals("Video is missing")
        compose.onNodeWithTag("yt_video_toggle").assertIsNotEnabled()
    }
    @Test fun fullscreenButtonAndBackExitFullscreen() {
        var exited = 0
        compose.setContent { TinkTheme { YtVideoContent(YtVideoState(loading = false), fullscreen = true, onFullscreen = { exited++ }) } }
        compose.onNodeWithTag("yt_video_fullscreen").performClick()
        compose.onNodeWithTag("yt_video_back").performClick()
        assertEquals(2, exited)
    }
}
