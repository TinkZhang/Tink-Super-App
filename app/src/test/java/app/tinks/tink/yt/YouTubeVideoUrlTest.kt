package app.tinks.tink.yt

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class YouTubeVideoUrlTest {
    @Test fun acceptsSingleVideoLinksAndStripsTrackingAndPlaylists() {
        listOf(
            "https://youtu.be/LbCHAQ7HKu4?si=k3IuU5o_SqidYee5",
            "https://www.youtube.com/watch?v=LbCHAQ7HKu4&list=PL123&index=2",
            "https://m.youtube.com/watch?v=LbCHAQ7HKu4",
            "https://music.youtube.com/watch?v=LbCHAQ7HKu4",
            "https://youtube.com/shorts/LbCHAQ7HKu4",
            "https://youtube.com/live/LbCHAQ7HKu4",
            " https://youtube.com/embed/LbCHAQ7HKu4 ",
        ).forEach { assertEquals(it, "https://www.youtube.com/watch?v=LbCHAQ7HKu4", YouTubeVideoUrl.parse(it)?.canonicalUrl) }
    }

    @Test fun rejectsOtherSitesSpoofedHostsPlaylistsAndMalformedLinks() {
        listOf(
            "https://vimeo.com/123", "https://youtube.com.evil.test/watch?v=LbCHAQ7HKu4",
            "https://youtube.com@evil.test/watch?v=LbCHAQ7HKu4", "https://evil.test@youtube.com/watch?v=LbCHAQ7HKu4",
            "file:///watch?v=LbCHAQ7HKu4", "javascript:alert(1)", "--exec=touch /tmp/file",
            "https://youtube.com:8080/watch?v=LbCHAQ7HKu4", "https://youtube.com/playlist?list=PL123",
            "https://youtube.com/@channel", "https://youtu.be/LbCHAQ7HKu4/extra",
            "https://youtube.com/watch?v=LbCHAQ7HKu4&v=aaaaaaaaaaa", "https://youtu.be/short",
            "https://youtube.com/watch?v=../../file", "not a url", "",
        ).forEach { assertNull(it, YouTubeVideoUrl.parse(it)) }
    }

    @Test fun requestsOnlyYoutubeWithFixedAudioAndVideoProfiles() {
        val item = YtDownload(id = 1, videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.AUDIO, attempt = "one")
        val audio = youtubeDownloadRequest(item, File("/tmp/yt/1"))
        val video = youtubeDownloadRequest(item.copy(kind = YtDownloadKind.VIDEO), File("/tmp/yt/1"))
        assertEquals("youtube", audio.getOption("--use-extractors"))
        assertTrue(audio.hasOption("--no-playlist"))
        assertTrue(audio.hasOption("--ignore-config"))
        assertEquals("m4a", audio.getOption("--audio-format"))
        assertEquals("mp4", video.getOption("--remux-video"))
        assertTrue(video.getOption("-f")!!.contains("height<=720"))
        val args = audio.buildCommand()
        val print = args.indexOf("--print-to-file")
        assertEquals(listOf("--print-to-file", "after_move:%(filepath)s", "/tmp/yt/1/completed.txt"), args.subList(print, print + 3))
        assertEquals("https://www.youtube.com/watch?v=LbCHAQ7HKu4", args.last())
    }
}
