package app.tinks.tink.yt

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtPlaybackCompletion @Inject constructor(private val db: YtDatabase, private val files: YtMediaFiles) {
    /** Returns true when the finished episode should leave the active subscription queue. */
    suspend fun complete(id: Int, group: String): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            val item = db.downloads().get(id) ?: return@withTransaction false
            if (item.status != YtDownloadStatus.READY) return@withTransaction false
            db.downloads().playback(id, 0, true)
            val memberships = db.subscriptions().memberships(item.videoId, item.kind)
            val active = memberships.find { it.playlistId == group } ?: return@withTransaction false
            if (!active.autoDelete) return@withTransaction false
            val keepFile = item.standalone || memberships.any { !it.autoDelete }
            if (keepFile) {
                db.subscriptions().dismissFrom(group, item.videoId, item.kind)
            } else {
                // READY files cannot be retried or written by a downloader. The transaction excludes
                // playlist imports/manual deletion while removing the file and saving tombstones.
                val directory = files.directory(id)
                check(!directory.exists() || directory.deleteRecursively()) { "Could not remove the played episode." }
                db.subscriptions().dismiss(item.videoId, item.kind)
                db.downloads().delete(id)
            }
            YtDownloadLog.info(id, null, "playback.completed", "autoDelete=true fileKept=$keepFile group=$group")
            true
        }
    }
}
