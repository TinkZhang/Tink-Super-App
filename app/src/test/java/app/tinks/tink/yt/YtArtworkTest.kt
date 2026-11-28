package app.tinks.tink.yt

import org.junit.Assert.*
import org.junit.Test

class YtArtworkTest {
    @Test fun onlyYouTubeImageHostsAreAccepted() {
        assertEquals("https://i.ytimg.com/vi/LbCHAQ7HKu4/hqdefault.jpg", youtubeThumbnail("LbCHAQ7HKu4"))
        assertNull(youtubeThumbnail("../../other"))
        assertNotNull(youtubeArtworkUrl("https://yt3.googleusercontent.com/avatar"))
        assertNotNull(youtubeArtworkUrl("https://yt3.ggpht.com/avatar"))
        listOf("http://i.ytimg.com/a", "https://i.ytimg.com.evil.test/a", "https://secret@i.ytimg.com/a",
            "https://localhost/a", "file:///private/image", "https://i.ytimg.com:8443/a").forEach { assertNull(it, youtubeArtworkUrl(it)) }
    }
}
