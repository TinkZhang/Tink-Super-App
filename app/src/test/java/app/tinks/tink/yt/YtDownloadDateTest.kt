package app.tinks.tink.yt

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class YtDownloadDateTest {
    @Test fun completionTimeIsWrittenOnlyByCurrentAttemptAndSurvivesPlaybackUpdates() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, YtDatabase::class.java).build()
        try {
            val id = db.downloads().insert(YtDownload(videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.AUDIO, attempt = "current", createdAt = 1)).toInt()
            assertNull(db.downloads().get(id)!!.downloadedAt)
            assertEquals(0, db.downloads().complete(id, "stale", "Wrong", "/file", 10, 100))
            assertNull(db.downloads().get(id)!!.downloadedAt)
            assertEquals(1, db.downloads().complete(id, "current", "Episode", "/file", 10, 200))
            db.downloads().playback(id, 1000, false)
            assertEquals(200L, db.downloads().get(id)!!.downloadedAt)
            db.downloads().markMissing(id)
            db.downloads().retry(id, "retry")
            assertNull(db.downloads().get(id)!!.downloadedAt)
            db.downloads().complete(id, "retry", "Episode", "/file", 10, 300)
            assertEquals(300L, db.downloads().get(id)!!.downloadedAt)
        } finally { db.close() }
    }
    @Test fun legacyDatesAreClearlyIdentifiedAsAddedDates() {
        val item = YtDownload(videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.AUDIO, attempt = "a", createdAt = 1)
        assertTrue(downloadDateLabel(item).startsWith("Added "))
        assertTrue(downloadDateLabel(item.copy(downloadedAt = 100)).startsWith("Downloaded "))
    }
}
