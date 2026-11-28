package app.tinks.tink.yt

import android.app.Application
import androidx.room.Room
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
class YtLibraryDeletionTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private lateinit var db: YtDatabase
    private val files = YtMediaFiles(context)
    private val cancellations = mutableListOf<Int>()
    private lateinit var deletion: YtLibraryDeletion
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, YtDatabase::class.java).build()
        val scheduler = object : YtDownloadScheduler {
            override fun schedule(download: YtDownload) = true
            override fun cancel(id: Int) { cancellations += id }
            override fun pendingAttempts() = emptyMap<Int, String>()
        }
        deletion = YtLibraryDeletion(db, files, YtDownloadRepository(db.downloads(), scheduler, files), YtNativeGate())
    }
    @After fun close() { db.close(); File(context.filesDir, "yt").deleteRecursively() }

    private suspend fun episode(kind: YtDownloadKind = YtDownloadKind.AUDIO): YtDownload {
        val id = db.downloads().insert(YtDownload(videoId = "LbCHAQ7HKu4", kind = kind, attempt = kind.name, standalone = false)).toInt()
        val media = File(files.directory(id).apply { mkdirs() }, if (kind == YtDownloadKind.AUDIO) "media.m4a" else "media.mp4").apply { writeText("fixture") }
        File(media.parentFile, "cover.jpg").writeText("art")
        db.downloads().complete(id, kind.name, "Episode", media.absolutePath, media.length())
        if (db.subscriptions().get("p") == null) db.subscriptions().insert(YtSubscription("p", "Playlist"))
        db.subscriptions().addEntry(YtPlaylistEntry("p", "LbCHAQ7HKu4", "Episode", 0, kind = kind))
        return db.downloads().get(id)!!
    }

    @Test fun removePlaylistWithDownloadsDeletesAudioVideoAndFilesWithoutIndividuals() = runBlocking {
        val audio = episode()
        val video = episode(YtDownloadKind.VIDEO)
        val removed = detachYtPlaylist(db, "p", true)
        deletion.delete(removed.map { it.id })
        assertNull(db.subscriptions().get("p"))
        assertTrue(db.subscriptions().entries("p").isEmpty())
        assertTrue(db.downloads().all().isEmpty())
        assertFalse(files.directory(audio.id).exists())
        assertFalse(files.directory(video.id).exists())
    }

    @Test fun keepOnlyPromotesMatchingFormatAndNotDismissedMemberships() = runBlocking {
        val audio = episode()
        val video = episode(YtDownloadKind.VIDEO)
        db.subscriptions().dismiss(video.videoId, video.kind)
        detachYtPlaylist(db, "p", false)
        assertTrue(db.downloads().get(audio.id)!!.standalone)
        assertFalse(db.downloads().get(video.id)!!.standalone)
        assertTrue(files.completedFile(audio)!!.exists())
        assertNull(db.subscriptions().get("p"))
    }

    @Test fun bulkDeleteDeduplicatesSharedDownloadsAndKeepsOtherFormat() = runBlocking {
        val audio = episode()
        val video = episode(YtDownloadKind.VIDEO)
        db.subscriptions().insert(YtSubscription("other", "Other"))
        db.subscriptions().addEntry(YtPlaylistEntry("other", audio.videoId, audio.title, 0))
        deletion.delete(listOf(audio.id, audio.id))
        assertEquals(listOf(audio.id), cancellations)
        assertNull(db.downloads().get(audio.id))
        assertTrue(db.subscriptions().entries("other").single().dismissed)
        assertTrue(db.subscriptions().entries("p").first { it.kind == YtDownloadKind.AUDIO }.dismissed)
        assertNotNull(db.downloads().get(video.id))
        assertTrue(files.completedFile(video)!!.exists())
    }

    @Test fun deletionWorksWhenFileAlreadyMissingAndIsSafeToRepeat() = runBlocking {
        val audio = episode()
        files.directory(audio.id).deleteRecursively()
        deletion.delete(listOf(audio.id))
        deletion.delete(listOf(audio.id))
        assertTrue(db.downloads().all().isEmpty())
    }
}
