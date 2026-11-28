package app.tinks.tink.yt

import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.*

class YtPlaylistImportTest {
    private val entries = (0 until 1000).map { YtPlaylistEntry("p", "video$it", "Episode $it", it) }
    @Test fun regularChecksUseOnlyTheHeadEvenIfAnOlderPositionHasANewerDate() {
        val sub = YtSubscription("p", "Playlist")
        val dates = entries.associate { it.videoId to it.position.toLong() }
        assertEquals(1, playlistScanLimit(sub))
        assertEquals("video0", selectedPlaylistEpisodes(sub, entries, dates).single().videoId)
        assertEquals(30, playlistScanLimit(sub.copy(latestCount = 30)))
    }
    @Test fun allOnceSelectsEveryMatchingEntryWithoutAnyDateLookups() {
        val sub = YtSubscription("p", "Archive", downloadAllOnce = true, enabled = false)
        assertNull(playlistScanLimit(sub))
        assertEquals(1000, selectedPlaylistEpisodes(sub, entries, emptyMap()).size)
        assertEquals(10, selectedPlaylistEpisodes(sub.copy(titleRegex = "Episode [0-9]$"), entries, emptyMap()).size)
        assertEquals(1000, selectedPlaylistEpisodes(sub, entries + entries, emptyMap()).size)
    }
    @Test fun overlappingCheckIsCoalescedAndCancellationAllowsRetry() = runBlocking {
        val checks = YtPlaylistChecks()
        val hold = CompletableDeferred<Unit>()
        val owner = launch(start = CoroutineStart.UNDISPATCHED) { checks.run("p") { hold.await() } }
        assertFalse(checks.run("p") { fail("must not scan twice") })
        assertTrue(checks.run("other") {})
        owner.cancelAndJoin()
        assertTrue(checks.run("p") {})
    }
}
