package app.tinks.tink.yt

import org.junit.Assert.*
import org.junit.Test

class YtLabelsTest {
    @Test fun normalizesWhitespaceAndDeduplicatesCaseInsensitively() {
        assertEquals(listOf("History", "Long interviews"), parseEpisodeLabels(" History, history, Long   interviews, ,"))
        assertTrue(parseEpisodeLabels(" , \n ").isEmpty())
        assertTrue(runCatching { parseEpisodeLabels("x".repeat(41)) }.isFailure)
        assertTrue(runCatching { parseEpisodeLabels((1..11).joinToString(",")) }.isFailure)
    }
    @Test fun labelsSelectOnlyMatchingReadyIndividualAudioWithoutDuplicatingIt() {
        val item = YtDownload(1, "LbCHAQ7HKu4", YtDownloadKind.AUDIO, "one", status = YtDownloadStatus.READY, labels = "History\nInterviews", createdAt = 20)
        val older = item.copy(id = 2, createdAt = 10, labels = "history")
        val items = listOf(item, older, item.copy(id = 3, standalone = false), item.copy(id = 4, kind = YtDownloadKind.VIDEO), item.copy(id = 5, status = YtDownloadStatus.FAILED))
        assertEquals(listOf(2, 1), libraryEpisodes(items, emptyList(), "label:history").map { it.id })
        assertEquals(listOf(1), libraryEpisodes(items, emptyList(), "label:interviews").map { it.id })
        assertTrue(libraryEpisodes(listOf(item.copy(labels = "")), emptyList(), "label:history").isEmpty())
        assertEquals(listOf("History", "Interviews"), libraryLabels(items))
    }
    @Test fun newSubscriptionsDefaultToOneLatestEpisode() {
        assertEquals(1, YtSubscription("p", "Playlist").latestCount)
    }
}
