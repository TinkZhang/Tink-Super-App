package app.tinks.tink.yt

import org.junit.Assert.*
import org.junit.Test

class YtVideoTest {
    @Test fun onlyReadyVideoCanOpenInTheVideoPlayer() {
        val item = YtDownload(videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.VIDEO, attempt = "a", status = YtDownloadStatus.READY)
        assertTrue(isLocalVideo(item))
        assertFalse(isLocalVideo(item.copy(kind = YtDownloadKind.AUDIO)))
        YtDownloadStatus.entries.filter { it != YtDownloadStatus.READY }.forEach { assertFalse(isLocalVideo(item.copy(status = it))) }
    }
    @Test fun unfinishedVideoResumesWhileCompletedVideoRestarts() {
        assertEquals(54_000L to false, videoProgress(54_000, false))
        assertEquals(0L to false, videoProgress(-1, false))
        assertEquals(0L to true, videoProgress(100_000, true))
    }
}
