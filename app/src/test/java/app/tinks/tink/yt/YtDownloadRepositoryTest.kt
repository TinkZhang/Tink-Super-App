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
class YtDownloadRepositoryTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private lateinit var db: YtDatabase
    private lateinit var repository: YtDownloadRepository
    private val scheduler = FakeYtScheduler()
    private val files = YtMediaFiles(context)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, YtDatabase::class.java).build()
        repository = YtDownloadRepository(db.downloads(), scheduler, files)
    }
    @After fun tearDown() { db.close(); File(context.filesDir, "yt").deleteRecursively() }

    @Test fun storesSeparateAudioAndVideoAndDeduplicatesTrackedUrls() = runBlocking {
        repository.download(URL, YtDownloadKind.AUDIO)
        repository.download(URL, YtDownloadKind.VIDEO)
        assertEquals(2, db.downloads().all().size)
        assertEquals(2, scheduler.pending.size)
        assertTrue(runCatching { repository.download("https://www.youtube.com/watch?v=LbCHAQ7HKu4", YtDownloadKind.AUDIO) }.isFailure)
        assertEquals(2, db.downloads().all().size)
    }

    @Test fun rejectsOtherSourcesBeforePersistenceOrScheduling() = runBlocking {
        assertTrue(runCatching { repository.download("https://vimeo.com/123", YtDownloadKind.AUDIO) }.isFailure)
        assertTrue(db.downloads().all().isEmpty())
        assertTrue(scheduler.pending.isEmpty())
    }

    @Test fun cancelledAndSupersededAttemptsCannotOverwriteRetry() = runBlocking {
        repository.download(URL, YtDownloadKind.AUDIO)
        val first = db.downloads().all().single()
        repository.cancel(first.id)
        assertEquals(YtDownloadStatus.CANCELLED, db.downloads().get(first.id)!!.status)
        assertEquals(0, db.downloads().complete(first.id, first.attempt, "Late result", "/wrong", 123))
        repository.retry(first.id)
        val retry = db.downloads().get(first.id)!!
        assertNotEquals(first.attempt, retry.attempt)
        assertEquals(0, db.downloads().updateActive(first.id, first.attempt, YtDownloadStatus.FAILED, 0, "Old failure"))
        assertEquals(YtDownloadStatus.QUEUED, db.downloads().get(first.id)!!.status)
    }

    @Test fun rejectedScheduleIsRetryableAndInterruptedJobsAreRecovered() = runBlocking {
        scheduler.accept = false
        assertTrue(runCatching { repository.download(URL, YtDownloadKind.AUDIO) }.isFailure)
        val id = db.downloads().all().single().id
        assertEquals(YtDownloadStatus.FAILED, db.downloads().get(id)!!.status)
        scheduler.accept = true
        repository.retry(id)
        repository.reconcile()
        assertEquals(YtDownloadStatus.QUEUED, db.downloads().get(id)!!.status)
        scheduler.pending.clear()
        repository.reconcile()
        assertEquals(YtDownloadStatus.FAILED, db.downloads().get(id)!!.status)
    }

    @Test fun missingFilesBecomeRetryableAndPathsCannotEscapeMediaFolder() = runBlocking {
        repository.download(URL, YtDownloadKind.AUDIO)
        val item = db.downloads().all().single()
        val directory = files.directory(item.id).apply { mkdirs() }
        val media = File(directory, "media.m4a").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        db.downloads().complete(item.id, item.attempt, "Video title", media.absolutePath, media.length())
        assertNotNull(files.completedFile(db.downloads().get(item.id)!!))
        val outside = File(context.filesDir, "outside.m4a").apply { writeText("private") }
        try { assertNull(files.completedFile(item.copy(filePath = outside.absolutePath))) }
        finally { outside.delete() }
        media.delete()
        repository.reconcile()
        assertEquals(YtDownloadStatus.FAILED, db.downloads().get(item.id)!!.status)
    }

    @Test fun runnerPublishesReadyOnlyAfterEngineReturnsAndMapsFailures() = runBlocking {
        repository.download(URL, YtDownloadKind.AUDIO)
        val item = db.downloads().all().single()
        val engine = object : YtDownloadEngine {
            override suspend fun download(item: YtDownload, onProgress: (Int, Boolean) -> Unit): YtDownloadResult {
                onProgress(35, false)
                assertEquals(YtDownloadStatus.DOWNLOADING, db.downloads().get(item.id)!!.status)
                val file = File(files.directory(item.id).apply { mkdirs() }, "media.m4a").apply { writeText("fixture") }
                return YtDownloadResult("Finished title", file)
            }
            override fun cancel(attempt: String) {}
        }
        YtDownloadRunner(db.downloads(), engine).run(item.id, item.attempt) { _, _ -> }
        assertEquals(YtDownloadStatus.READY, db.downloads().get(item.id)!!.status)
        assertEquals("Finished title", db.downloads().get(item.id)!!.title)
        assertTrue(youtubeDownloadError(Exception("HTTP Error 403")).contains("blocked"))
        assertTrue(youtubeDownloadError(Exception("No space left on device")).contains("storage"))
    }

    @Test fun backgroundQueueSchedulesOnlyAvailableSlotsAndDoesNotMarkWaitingRowsFailed() = runBlocking {
        repeat(8) { index ->
            db.downloads().insert(YtDownload(videoId = "video$index", kind = YtDownloadKind.AUDIO,
                attempt = "attempt$index", standalone = false, background = true))
        }
        repository.scheduleBackgroundQueue()
        assertEquals(4, scheduler.pending.size)
        repository.reconcile()
        assertTrue(db.downloads().all().all { it.status == YtDownloadStatus.QUEUED })
        val done = db.downloads().all().first { it.id in scheduler.pending }
        db.downloads().complete(done.id, done.attempt, "Done", "/file", 123)
        scheduler.pending.remove(done.id)
        repository.scheduleBackgroundQueue()
        assertEquals(4, scheduler.pending.size)
        assertFalse(done.id in scheduler.pending)
        val cancelled = scheduler.pending.keys.first()
        repository.cancel(cancelled)
        assertEquals(4, scheduler.pending.size)
        assertFalse(cancelled in scheduler.pending)
        assertEquals(YtDownloadStatus.CANCELLED, db.downloads().get(cancelled)!!.status)
    }

    @Test fun individuallyAddingAPlaylistEpisodeMakesItAvailableUngroupedWithoutRedownloading() = runBlocking {
        db.downloads().insert(YtDownload(videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.AUDIO,
            attempt = "playlist", standalone = false, background = true))
        repository.download(URL, YtDownloadKind.AUDIO)
        assertTrue(db.downloads().all().single().standalone)
        assertTrue(scheduler.pending.isEmpty())
    }

    companion object { const val URL = "https://youtu.be/LbCHAQ7HKu4?si=k3IuU5o_SqidYee5" }
}

private class FakeYtScheduler : YtDownloadScheduler {
    val pending = mutableMapOf<Int, String>()
    var accept = true
    override fun schedule(download: YtDownload): Boolean {
        if (accept) pending[download.id] = download.attempt
        return accept
    }
    override fun cancel(id: Int) { pending.remove(id) }
    override fun pendingAttempts() = pending.toMap()
}
