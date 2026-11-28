package app.tinks.tink.yt

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class YtPlaybackCompletionTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private lateinit var db: YtDatabase
    private val files = YtMediaFiles(context)
    private lateinit var completion: YtPlaybackCompletion
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, YtDatabase::class.java).build()
        completion = YtPlaybackCompletion(db, files)
    }
    @After fun close() { db.close(); File(context.filesDir, "yt").deleteRecursively() }

    private suspend fun episode(standalone: Boolean = false, autoDelete: Boolean = true): YtDownload {
        db.subscriptions().insert(YtSubscription("p", "Playlist", autoDelete = autoDelete))
        val id = db.downloads().insert(YtDownload(videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.AUDIO,
            attempt = "one", standalone = standalone)).toInt()
        val folder = files.directory(id).apply { mkdirs() }
        val audio = File(folder, "media.m4a").apply { writeText("audio fixture") }
        File(folder, "cover.jpg").writeText("art fixture")
        db.downloads().complete(id, "one", "Episode", audio.absolutePath, audio.length())
        db.subscriptions().addEntry(YtPlaylistEntry("p", "LbCHAQ7HKu4", "Episode", 0))
        return db.downloads().get(id)!!
    }

    @Test fun defaultOnRemovesAudioAndArtworkAndDoesNotRedownloadIt() = runBlocking {
        val item = episode()
        assertTrue(completion.complete(item.id, "p"))
        assertNull(db.downloads().get(item.id))
        assertFalse(files.directory(item.id).exists())
        assertTrue(db.subscriptions().entries("p").single().dismissed)
        assertFalse(completion.complete(item.id, "p"))
        db.withTransaction { enqueueLatestPlaylistEpisodes(db, db.subscriptions().get("p")!!,
            listOf(YtPlaylistEntry("p", item.videoId, item.title, 0)), mapOf(item.videoId to 123L)) }
        assertTrue(db.downloads().all().isEmpty())
    }

    @Test fun disabledAutoDeleteKeepsFileAndMarksPlayed() = runBlocking {
        val item = episode(autoDelete = false)
        assertFalse(completion.complete(item.id, "p"))
        assertTrue(files.completedFile(item)!!.exists())
        assertTrue(db.downloads().get(item.id)!!.played)
        assertFalse(db.subscriptions().entries("p").single().dismissed)
    }

    @Test fun readsLatestSettingAtCompletionAndPreservesStandaloneCopy() = runBlocking {
        val item = episode(standalone = true)
        db.subscriptions().edit("p", "", true, "0 8 * * *", 10, 0, "UTC", false, 1.5f)
        assertFalse(completion.complete(item.id, "p"))
        db.subscriptions().edit("p", "", true, "0 8 * * *", 10, 0, "UTC", true, 1.5f)
        assertTrue(completion.complete(item.id, "p"))
        assertNotNull(db.downloads().get(item.id))
        assertTrue(files.completedFile(item)!!.exists())
        assertTrue(db.subscriptions().entries("p").single().dismissed)
    }

    @Test fun anotherSubscriptionWithKeepEnabledProtectsSharedFile() = runBlocking {
        val item = episode()
        db.subscriptions().insert(YtSubscription("other", "Keep", autoDelete = false))
        db.subscriptions().addEntry(YtPlaylistEntry("other", item.videoId, "Episode", 0))
        assertTrue(completion.complete(item.id, "p"))
        assertNotNull(db.downloads().get(item.id))
        assertTrue(files.completedFile(item)!!.exists())
        assertFalse(db.subscriptions().entries("other").single().dismissed)
    }

    @Test fun individualPlaybackAndUnknownSubscriptionNeverDeleteStandaloneAudio() = runBlocking {
        val item = episode(standalone = true)
        assertFalse(completion.complete(item.id, "individual"))
        assertFalse(completion.complete(item.id, "removed"))
        assertTrue(files.completedFile(item)!!.exists())
    }

    @Test fun nonReadyEpisodeCannotBeCompletedOrDeleted() = runBlocking {
        val item = episode()
        db.downloads().markMissing(item.id)
        assertFalse(completion.complete(item.id, "p"))
        assertNotNull(db.downloads().get(item.id))
        assertTrue(files.directory(item.id).exists())
    }
}
