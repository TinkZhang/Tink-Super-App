package app.tinks.tink.yt

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class YtPlaylistImportDatabaseTest {
    @Test fun thousandEpisodeImportPersistsQueueAndDoesNotDuplicateOnRetry() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, YtDatabase::class.java).build()
        try {
            val sub = YtSubscription("p", "Archive", enabled = false, downloadAllOnce = true, format = YtSubscriptionFormat.VIDEO)
            db.subscriptions().insert(sub)
            val entries = (0 until 1000).map { YtPlaylistEntry("p", "video$it", "Episode $it", it) }
            assertEquals(1000, db.withTransaction { enqueueLatestPlaylistEpisodes(db, sub, entries, emptyMap()) })
            assertEquals(0, db.withTransaction { enqueueLatestPlaylistEpisodes(db, sub, entries, emptyMap()) })
            assertEquals(1000, db.downloads().all().size)
            assertTrue(db.downloads().all().all { it.background && it.kind == YtDownloadKind.VIDEO })
            assertTrue(db.subscriptions().get("p")!!.downloadAllOnce)
            assertFalse(db.subscriptions().get("p")!!.enabled)
        } finally { db.close() }
    }
}
