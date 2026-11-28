package app.tinks.tink.yt

import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/** Detach atomically so a finishing playlist check cannot recreate deleted downloads. */
internal suspend fun detachYtPlaylist(db: YtDatabase, id: String, deleteDownloads: Boolean): List<YtDownload> = db.withTransaction {
    val items = db.subscriptions().entries(id).filterNot { it.dismissed }.mapNotNull {
        db.downloads().find(it.videoId, it.kind)
    }.distinctBy { it.id }
    items.forEach {
        if (deleteDownloads) db.subscriptions().dismiss(it.videoId, it.kind)
        else db.downloads().makeStandalone(it.videoId, it.kind)
    }
    db.subscriptions().deleteEntries(id)
    db.subscriptions().delete(id)
    items
}

class YtLibraryDeletion @Inject constructor(
    private val db: YtDatabase,
    private val files: YtMediaFiles,
    private val downloads: YtDownloadRepository,
    private val gate: YtNativeGate,
) {
    suspend fun delete(ids: List<Int>) {
        val unique = ids.distinct()
        // Cancellation must happen before waiting for the native downloader's lock.
        unique.forEach { downloads.cancel(it) }
        var failed = 0
        unique.forEach { id ->
            try {
                gate.run(id, null, "delete") {
                    db.withTransaction {
                        val item = db.downloads().get(id) ?: return@withTransaction
                        val directory = files.directory(id)
                        check(!directory.exists() || directory.deleteRecursively()) { "Could not delete the file." }
                        db.subscriptions().dismiss(item.videoId, item.kind)
                        db.downloads().delete(id)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                failed++
                YtDownloadLog.failure(id, null, "delete.failed", error)
            }
        }
        downloads.scheduleBackgroundQueue()
        check(failed == 0) { "Could not delete $failed downloads. Retry deleting them in Downloads." }
    }
}
