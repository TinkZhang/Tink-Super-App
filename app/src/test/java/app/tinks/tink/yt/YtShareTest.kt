package app.tinks.tink.yt

import org.junit.Assert.*
import org.junit.Test

class YtShareTest {
    @Test fun extractsCanonicalVideoLinkFromYouTubeSharedText() {
        val expected = "https://www.youtube.com/watch?v=LbCHAQ7HKu4"
        assertEquals(expected, youtubeUrlFromSharedText("https://youtu.be/LbCHAQ7HKu4?si=tracking"))
        assertEquals(expected, youtubeUrlFromSharedText("An interesting video\nhttps://www.youtube.com/watch?v=LbCHAQ7HKu4&list=PL1234567890"))
        assertEquals(expected, youtubeUrlFromSharedText("Watch this (https://youtube.com/shorts/LbCHAQ7HKu4)."))
    }
    @Test fun supportsPlaylistLinksButRejectsUnsupportedAndSpoofedSources() {
        assertEquals("https://www.youtube.com/playlist?list=PL1234567890", youtubeUrlFromSharedText("https://www.youtube.com/playlist?list=PL1234567890"))
        listOf(null, "hello", "file:///video.mp4", "https://youtube.com.evil.com/watch?v=LbCHAQ7HKu4", "https://youtube.com@evil.com/watch?v=LbCHAQ7HKu4", "https://example.com/?url=https://youtu.be/LbCHAQ7HKu4").forEach {
            assertNull(youtubeUrlFromSharedText(it))
        }
    }
}
