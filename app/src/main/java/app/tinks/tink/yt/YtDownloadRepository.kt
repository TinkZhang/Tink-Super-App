package app.tinks.tink.yt

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface YtDownloads {
    val downloads: Flow<List<YtDownload>>
    suspend fun download(url: String, kind: YtDownloadKind)
    suspend fun retry(id: Int)
    suspend fun cancel(id: Int)
    suspend fun reconcile()
}

interface YtDownloadScheduler {
    fun schedule(download: YtDownload): Boolean
    fun cancel(id: Int)
    fun pendingAttempts(): Map<Int, String>
}

@Singleton
class YtDownloadRepository @Inject constructor(
    private val dao: YtDownloadDao,
    private val scheduler: YtDownloadScheduler,
    private val files: YtMediaFiles,
) : YtDownloads {
    private val changes = Mutex()
    override val downloads = dao.observeAll()

    override suspend fun download(url: String, kind: YtDownloadKind) = changes.withLock {
        val video = requireNotNull(YouTubeVideoUrl.parse(url)) { "Enter a valid YouTube video link." }
        val existing = dao.find(video.videoId, kind)
        if (existing != null && !existing.standalone) {
            dao.makeStandalone(existing.videoId, existing.kind)
            return@withLock
        }
        require(existing == null) {
            if (existing?.status == YtDownloadStatus.READY) "This ${kind.label.lowercase()} is already downloaded."
            else "This ${kind.label.lowercase()} is already in your downloads. Use Retry if it stopped."
        }
        val item = YtDownload(videoId = video.videoId, kind = kind, attempt = newAttempt())
        val id = dao.insert(item)
        check(id in 1..Int.MAX_VALUE.toLong()) { "This download is already in the list." }
        YtDownloadLog.info(id.toInt(), item.attempt, "queue.created", "videoId=${item.videoId} kind=$kind")
        schedule(item.copy(id = id.toInt()))
    }

    override suspend fun retry(id: Int) = changes.withLock {
        val attempt = newAttempt()
        YtDownloadLog.info(id, attempt, "queue.retry")
        if (dao.retry(id, attempt) == 1) schedule(requireNotNull(dao.get(id)))
    }

    override suspend fun cancel(id: Int) {
        changes.withLock {
            dao.get(id)?.let {
                YtDownloadLog.info(id, it.attempt, "queue.cancel", "status=${it.status}")
                dao.updateActive(id, it.attempt, YtDownloadStatus.CANCELLED, it.progress)
                scheduler.cancel(id)
            }
        }
        scheduleBackgroundQueue()
    }

    override suspend fun reconcile() = changes.withLock {
        val pending = scheduler.pendingAttempts()
        dao.all().forEach { item ->
            if (item.status.isActive && !(item.background && item.status == YtDownloadStatus.QUEUED) && pending[item.id] != item.attempt) {
                YtDownloadLog.info(item.id, item.attempt, "queue.interrupted", "status=${item.status} no matching Android job")
                dao.updateActive(item.id, item.attempt, YtDownloadStatus.FAILED, item.progress,
                    "Download interrupted. Tap Retry to continue.")
            } else if (item.status == YtDownloadStatus.READY &&
                withContext(Dispatchers.IO) { files.completedFile(item) == null }
            ) {
                dao.markMissing(item.id)
                YtDownloadLog.info(item.id, item.attempt, "queue.fileMissing")
            }
        }
    }

    suspend fun scheduleBackgroundQueue() = changes.withLock {
        val pending = scheduler.pendingAttempts()
        val slots = (4 - pending.size).coerceAtLeast(0)
        dao.all().filter { it.background && it.status == YtDownloadStatus.QUEUED && it.id !in pending }
            .sortedBy { it.id }.take(slots).forEach { item ->
                try { schedule(item) } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* The row retains the error and offers Retry. */ }
            }
    }

    private suspend fun schedule(item: YtDownload) {
        try {
            check(scheduler.schedule(item)) { "Android could not start this download. Keep YT open and tap Retry." }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            YtDownloadLog.failure(item.id, item.attempt, "schedule.failed", error)
            dao.updateActive(item.id, item.attempt, YtDownloadStatus.FAILED, 0,
                "Android could not start this download. Keep YT open and tap Retry.")
            throw IllegalStateException("Could not start the download. Tap Retry.", error)
        }
    }

    private fun newAttempt() = UUID.randomUUID().toString()
}
