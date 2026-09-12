package app.tinks.tink.yt

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
        try {
            execution.withLock {
                val item = dao.get(id)?.takeIf { it.attempt == attempt && it.status.isActive } ?: return
                if (dao.updateActive(id, attempt, YtDownloadStatus.DOWNLOADING, item.progress) != 1) return
                val result = engine.download(item) { progress, processing ->
                    runBlocking {
                        dao.updateActive(id, attempt,
                            if (processing) YtDownloadStatus.PROCESSING else YtDownloadStatus.DOWNLOADING,
                            progress)
                    }
                    notify(progress, processing)
                }
                dao.complete(id, attempt, result.title, result.file.absolutePath, result.file.length())
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable + Dispatchers.IO) {
                engine.cancel(attempt)
                dao.updateActive(id, attempt, YtDownloadStatus.QUEUED, 0)
            }
            throw cancelled
        } catch (error: Exception) {
            dao.updateActive(id, attempt, YtDownloadStatus.FAILED, 0, youtubeDownloadError(error))
        }
    }

    fun cancel(attempt: String) = engine.cancel(attempt)
}
