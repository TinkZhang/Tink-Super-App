package app.tinks.tink.yt

import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDownloadRunner @Inject constructor(
    private val dao: YtDownloadDao,
    private val engine: YtDownloadEngine,
) {
    // Serialize native work to limit CPU, storage and bandwidth use. Each queued job remains durable.
    private val execution = Mutex()

    suspend fun run(id: Int, attempt: String, notify: (Int, Boolean) -> Unit) {
        val started = SystemClock.elapsedRealtime()
        YtDownloadLog.info(id, attempt, "runner.waiting")
        try {
            execution.withLock {
                YtDownloadLog.info(id, attempt, "runner.acquired", "waitMs=${SystemClock.elapsedRealtime() - started}")
                val item = dao.get(id)?.takeIf { it.attempt == attempt && it.status.isActive }
                if (item == null) {
                    YtDownloadLog.info(id, attempt, "runner.skipped", "missing, inactive or superseded attempt")
                    return
                }
                if (dao.updateActive(id, attempt, YtDownloadStatus.DOWNLOADING, item.progress) != 1) {
                    YtDownloadLog.info(id, attempt, "runner.skipped", "state changed before download")
                    return
                }
                val result = engine.download(item) { progress, processing ->
                    runBlocking {
                        dao.updateActive(id, attempt,
                            if (processing) YtDownloadStatus.PROCESSING else YtDownloadStatus.DOWNLOADING,
                            progress)
                    }
                    notify(progress, processing)
                }
                val updated = dao.complete(id, attempt, result.title, result.file.absolutePath, result.file.length())
                YtDownloadLog.info(id, attempt, "runner.completed", "updatedRows=$updated bytes=${result.file.length()}")
            }
        } catch (cancelled: CancellationException) {
            YtDownloadLog.info(id, attempt, "runner.cancelled", "elapsedMs=${SystemClock.elapsedRealtime() - started}")
            withContext(NonCancellable + Dispatchers.IO) {
                engine.cancel(attempt)
                dao.updateActive(id, attempt, YtDownloadStatus.QUEUED, 0)
            }
            throw cancelled
        } catch (error: Exception) {
            // The exception contains yt-dlp's stderr, including verbose extraction/runtime diagnostics.
            YtDownloadLog.failure(id, attempt, "runner.failed", error)
            dao.updateActive(id, attempt, YtDownloadStatus.FAILED, 0, youtubeDownloadError(error))
        } finally {
            YtDownloadLog.info(id, attempt, "runner.finished", "elapsedMs=${SystemClock.elapsedRealtime() - started}")
        }
    }

    fun cancel(attempt: String) = engine.cancel(attempt)
}
