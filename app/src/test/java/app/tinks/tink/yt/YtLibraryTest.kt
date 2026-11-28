package app.tinks.tink.yt

import org.junit.Assert.*
import org.junit.Test

class YtLibraryTest {
    @Test fun playlistUrlsAreCanonicalizedAndOtherHostsRejected() {
        val id = "PL64Si-sSRnvZxhAwf2kxsqQXsg8Qd9FDG"
        assertEquals(id, youtubePlaylistId("https://www.youtube.com/watch?v=sO35ixeX0k8&list=$id,"))
        assertEquals(id, youtubePlaylistId("https://youtube.com/playlist?list=$id"))
        listOf("https://youtube.com.evil.test/playlist?list=$id", "https://evil@youtube.com/playlist?list=$id",
            "https://youtube.com:123/playlist?list=$id", "https://youtube.com/playlist?list=$id&list=$id",
            "file:///playlist?list=$id", "https://youtu.be/sO35ixeX0k8").forEach { assertNull(it, youtubePlaylistId(it)) }
    }

    @Test fun titleFiltersUseSubstringRegexAndRespectDismissedEpisodes() {
        val entries = listOf(
            YtPlaylistEntry("p", "one", "Weekly SCIENCE news", 0),
            YtPlaylistEntry("p", "two", "History of space", 1),
            YtPlaylistEntry("p", "three", "Music", 2),
            YtPlaylistEntry("p", "four", "Science deleted", 3, true),
        )
        assertEquals(listOf("one", "two"), playlistMatches(entries, "(?i)science|history").map { it.videoId })
        assertEquals(3, playlistMatches(entries, "").size)
        assertEquals(0, playlistMatches(entries, "^Science$").size)
        assertThrows(IllegalArgumentException::class.java) { titleFilter("[") }
    }

    @Test fun playbackQueuesExcludeUnfinishedAndVideoFilesAndPreservePlaylistOrder() {
        val one = YtDownload(1, "one", YtDownloadKind.AUDIO, "a", status = YtDownloadStatus.READY)
        val two = one.copy(id = 2, videoId = "two", standalone = false)
        val entries = listOf(YtPlaylistEntry("group", "two", "Second", 0), YtPlaylistEntry("group", "one", "First", 1))
        val downloads = listOf(one, two, one.copy(id = 3, status = YtDownloadStatus.FAILED), one.copy(id = 4, kind = YtDownloadKind.VIDEO))
        assertEquals(listOf(1, 2), libraryEpisodes(downloads, entries, "group").map { it.id })
        assertEquals(listOf(1), libraryEpisodes(downloads, entries, "individual").map { it.id })
        assertEquals(listOf(1, 2), libraryEpisodes(downloads, entries, "all").map { it.id })
        assertEquals(listOf(1), libraryEpisodes(downloads, listOf(entries[0].copy(dismissed = true), entries[1]), "group").map { it.id })
    }
    @Test fun firstEntriesWinRegardlessOfCachedDates() {
        val entries = listOf(
            YtPlaylistEntry("p", "old", "Episode old", 0),
            YtPlaylistEntry("p", "new", "Episode new", 1),
            YtPlaylistEntry("p", "new", "Episode duplicate", 2),
            YtPlaylistEntry("p", "middle", "Episode middle", 3),
            YtPlaylistEntry("p", "other", "Trailer", 4),
        )
        val dates = mapOf("old" to 1L, "new" to 3L, "middle" to 2L, "other" to 4L)
        assertEquals(listOf("old"), latestPlaylistEpisodes(entries, "old", 2).map { it.videoId })
        assertTrue(latestPlaylistEpisodes(entries, "middle", 2).isEmpty())
        val selected = latestPlaylistEpisodes(entries, "Episode", 2, dates)
        assertEquals(listOf("old", "new"), selected.map { it.videoId })
        val alreadyDownloaded = setOf("old", "new")
        assertTrue(selected.filter { it.videoId !in alreadyDownloaded }.isEmpty())
        // Dismissal does not fill the selection with an older episode.
        assertEquals(listOf("old", "new"), latestPlaylistEpisodes(
            entries.map { it.copy(dismissed = it.videoId == "new") }, "Episode", 2, dates).map { it.videoId })
    }

    @Test fun subscriptionPlaybackRunsOldestUploadFirstRegardlessOfPlaylistPosition() {
        val entries = listOf(YtPlaylistEntry("p", "new", "New", 0), YtPlaylistEntry("p", "old", "Old", 1))
        val items = listOf(YtDownload(1, "new", YtDownloadKind.AUDIO, "a", status = YtDownloadStatus.READY),
            YtDownload(2, "old", YtDownloadKind.AUDIO, "b", status = YtDownloadStatus.READY))
        assertEquals(listOf("old", "new"), libraryEpisodes(items, entries, "p", mapOf("new" to 2L, "old" to 1L)).map { it.videoId })
        assertTrue(YtSubscription("p", "Playlist").autoDelete)
        listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, 4f).forEach {
            assertThrows(IllegalArgumentException::class.java) { validatePlaybackSpeed(it) }
        }
    }

}
