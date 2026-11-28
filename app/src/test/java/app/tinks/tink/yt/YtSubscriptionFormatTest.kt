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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class YtSubscriptionFormatTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    @Test fun formatChangesKeepExistingFilesAndRepeatedChecksDoNotDuplicateEither() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, YtDatabase::class.java).build()
        try {
            val sub = YtSubscription("p", "Playlist", format = YtSubscriptionFormat.VIDEO)
            db.subscriptions().insert(sub)
            val entries = listOf(YtPlaylistEntry("p", "LbCHAQ7HKu4", "Episode", 0))
            val dates = mapOf("LbCHAQ7HKu4" to 123L)
            assertEquals(1, db.withTransaction { enqueueLatestPlaylistEpisodes(db, sub.copy(format = YtSubscriptionFormat.AUDIO), entries, dates) })
            assertEquals(1, db.withTransaction { enqueueLatestPlaylistEpisodes(db, sub, entries, dates) })
            assertEquals(0, db.withTransaction { enqueueLatestPlaylistEpisodes(db, sub, entries, dates) })
            assertEquals(setOf(YtDownloadKind.AUDIO, YtDownloadKind.VIDEO), db.downloads().all().map { it.kind }.toSet())
            assertEquals(2, db.subscriptions().entries("p").size)
            val ready = db.downloads().all().map { it.copy(status = YtDownloadStatus.READY) }
            assertEquals(1, libraryEpisodes(ready, db.subscriptions().entries("p"), "p", dates).size)
            assertEquals(YtDownloadKind.VIDEO, libraryEpisodes(ready, db.subscriptions().entries("p"), "p", dates, YtDownloadKind.VIDEO).single().kind)
        } finally { db.close() }
    }
    @Test fun switchingToVideoPreservesAudioAndAudioDismissalDoesNotBlockVideo() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, YtDatabase::class.java).build()
        try {
            val sub = YtSubscription("p", "Playlist")
            db.subscriptions().insert(sub)
            val entries = listOf(YtPlaylistEntry("p", "LbCHAQ7HKu4", "Episode", 0))
            val dates = mapOf("LbCHAQ7HKu4" to 123L)
            db.withTransaction { enqueueLatestPlaylistEpisodes(db, sub, entries, dates) }
            db.subscriptions().dismiss("LbCHAQ7HKu4", YtDownloadKind.AUDIO)
            db.subscriptions().edit("p", "", false, "0 8 * * *", 1, 0, "UTC", false, 1.5f, YtSubscriptionFormat.VIDEO)
            val edited = db.subscriptions().get("p")!!
            assertEquals(YtSubscriptionFormat.VIDEO, edited.format)
            assertEquals(1, db.withTransaction { enqueueLatestPlaylistEpisodes(db, edited, entries, dates) })
            assertEquals(2, db.downloads().all().size)
            assertFalse(db.subscriptions().entries("p").single { it.kind == YtDownloadKind.VIDEO }.dismissed)
        } finally { db.close() }
    }
    @Test fun completedSubscriptionVideoDeletesOnlyVideoAndPreservesAudio() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, YtDatabase::class.java).build()
        val files = YtMediaFiles(context)
        try {
            val sub = YtSubscription("p", "Playlist", format = YtSubscriptionFormat.VIDEO)
            db.subscriptions().insert(sub)
            db.withTransaction { enqueueLatestPlaylistEpisodes(db, sub, listOf(YtPlaylistEntry("p", "LbCHAQ7HKu4", "Episode", 0)), mapOf("LbCHAQ7HKu4" to 123L)) }
            db.withTransaction { enqueueLatestPlaylistEpisodes(db, sub.copy(format = YtSubscriptionFormat.AUDIO), listOf(YtPlaylistEntry("p", "LbCHAQ7HKu4", "Episode", 0)), mapOf("LbCHAQ7HKu4" to 123L)) }
            val video = db.downloads().all().single { it.kind == YtDownloadKind.VIDEO }
            val file = File(files.directory(video.id).apply { mkdirs() }, "media.mp4").apply { writeText("fixture") }
            db.downloads().complete(video.id, video.attempt, "Video", file.path, file.length())
            assertTrue(YtPlaybackCompletion(db, files).complete(video.id, "p"))
            assertEquals(YtDownloadKind.AUDIO, db.downloads().all().single().kind)
            assertFalse(db.subscriptions().entries("p").single { it.kind == YtDownloadKind.AUDIO }.dismissed)
            assertTrue(db.subscriptions().entries("p").single { it.kind == YtDownloadKind.VIDEO }.dismissed)
        } finally { db.close(); File(context.filesDir, "yt").deleteRecursively() }
    }
}
