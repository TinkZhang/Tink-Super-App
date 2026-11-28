package app.tinks.tink.yt

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class YtDlpUpdateCheckTest {
    private var time = 1_000_000L
    private var saved = 0L
    private var updates = 0
    private fun checker() = YtDlpUpdateCheck({ saved }, { saved = it }, { time })

    @Test fun firstCheckIsSavedAndSkippedAcrossInstancesUntil24Hours() {
        assertTrue(checker().run(true) { updates++ })
        assertEquals(time, saved)
        time += 86_400_000L - 1
        assertFalse(checker().run(true) { updates++ })
        time++
        assertTrue(checker().run(true) { updates++ })
        assertEquals(2, updates)
    }

    @Test fun failedUpdateIsNotCachedAndNextDownloadRetries() {
        assertThrows(IOException::class.java) {
            checker().run(true) { throw IOException("GitHub unavailable") }
        }
        assertEquals(0L, saved)
        assertTrue(checker().run(true) { updates++ })
        assertEquals(1, updates)
    }

    @Test fun missingInstalledVersionForcesCheckEvenWithRecentTimestamp() {
        saved = time
        assertTrue(checker().run(false) { updates++ })
        assertEquals(1, updates)
    }

    @Test fun clockMovingBackwardDoesNotSuppressUpdates() {
        saved = time + 1
        assertTrue(checker().run(true) { updates++ })
    }

    @Test fun cancellationPropagatesWithoutRecordingSuccess() {
        assertThrows(InterruptedException::class.java) {
            checker().run(true) { throw InterruptedException() }
        }
        assertEquals(0L, saved)
    }
}
