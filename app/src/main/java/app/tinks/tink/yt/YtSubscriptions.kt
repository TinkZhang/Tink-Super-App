package app.tinks.tink.yt

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.*
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class YtCheckState(val message: String, val busy: Boolean)

@Singleton
class YtSubscriptions @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: YtDatabase,
    private val gate: YtNativeGate,
    private val updater: YtDlpUpdater,
    private val downloads: YtDownloadRepository,
) {
    val subscriptions = db.subscriptions().observe()
    val entries = db.subscriptions().observeEntries()
    private val work get() = WorkManager.getInstance(context)
    val checks get() = work.getWorkInfosByTagFlow("yt-subscription-check").map(::subscriptionCheckStates)

    private val activeChecks = YtPlaylistChecks()
    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    suspend fun add(url: String, regex: String, cron: String, latestCount: Int, autoDelete: Boolean = true, speed: Float = 1f, format: YtSubscriptionFormat = YtSubscriptionFormat.AUDIO, downloadAllOnce: Boolean = false) {
        val id = requireNotNull(youtubePlaylistId(url)) { "Enter a YouTube playlist link, including list=…" }
        titleFilter(regex)
        val storedCron = if (downloadAllOnce) "0 8 * * *" else cron.trim()
        val storedCount = if (downloadAllOnce) latestCount.coerceIn(1, 500) else latestCount
        validateSubscriptionOptions(storedCron, storedCount)
        validatePlaybackSpeed(speed)
        require(db.subscriptions().get(id) == null) { "You already subscribe to this playlist." }
        db.subscriptions().insert(YtSubscription(id, "Playlist $id", regex, cron = storedCron, latestCount = storedCount, autoDelete = autoDelete, playbackSpeed = speed, format = format, downloadAllOnce = downloadAllOnce, enabled = !downloadAllOnce,
            nextCheck = YtCron.parse(storedCron).nextAfter(System.currentTimeMillis()), scheduleZone = ZoneId.systemDefault().id))
        if (!downloadAllOnce) scheduleChecks(id)
        checkNow(id)
    }

    // Retain the legacy unique name: UPDATE migrates installed daily WorkManager jobs to cron polling.
    fun scheduleChecks(id: String) {
        work.enqueueUniquePeriodicWork("yt-playlist-daily-$id", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<YtSubscriptionWorker>(15, TimeUnit.MINUTES)
                .setConstraints(network).addTag("yt-subscription-check").addTag("playlist:$id").setInputData(workDataOf("playlist" to id)).build())
    }

    suspend fun restoreSchedules() {
        db.subscriptions().all().forEach { if (it.enabled && !it.downloadAllOnce) scheduleChecks(it.playlistId) else work.cancelUniqueWork("yt-playlist-daily-${it.playlistId}") }
    }

    suspend fun runScheduled(id: String, retry: Boolean, progress: suspend (String) -> Unit = {}) {
        val subscription = db.subscriptions().get(id)?.takeIf { it.enabled && !it.downloadAllOnce } ?: return
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        if (subscription.nextCheck == 0L || subscription.scheduleZone != zone.id) {
            db.subscriptions().advanceSchedule(id, subscription.nextCheck, YtCron.parse(subscription.cron).nextAfter(now, zone), zone.id)
            return
        }
        if (!retry) {
            if (now < subscription.nextCheck) return
            if (db.subscriptions().advanceSchedule(id, subscription.nextCheck, YtCron.parse(subscription.cron).nextAfter(now, zone), zone.id) != 1) return
        }
        sync(id, progress = progress)
    }

    suspend fun checkNow(id: String) {
        require(db.subscriptions().get(id) != null) { "This subscription no longer exists." }
        YtDownloadLog.info(0, null, "playlist.requested", "playlist=$id")
        work.enqueueUniqueWork("yt-playlist-now-$id", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<YtSubscriptionWorker>().setConstraints(network)
                .addTag("yt-subscription-check").addTag("playlist:$id").addTag("manual").addTag("requested:${System.currentTimeMillis()}")
                .setInputData(workDataOf("playlist" to id, "manual" to true)).build()).result.let { operation -> withContext(Dispatchers.IO) { operation.get() } }
    }

    suspend fun edit(id: String, regex: String, enabled: Boolean, cron: String, latestCount: Int, autoDelete: Boolean = true, speed: Float = 1f, format: YtSubscriptionFormat = YtSubscriptionFormat.AUDIO, downloadAllOnce: Boolean = false) {
        titleFilter(regex)
        val storedCron = if (downloadAllOnce) "0 8 * * *" else cron.trim()
        val storedCount = if (downloadAllOnce) latestCount.coerceIn(1, 500) else latestCount
        validateSubscriptionOptions(storedCron, storedCount)
        validatePlaybackSpeed(speed)
        val previous = db.subscriptions().get(id) ?: return
        db.subscriptions().edit(id, regex, enabled && !downloadAllOnce, storedCron, storedCount,
            YtCron.parse(storedCron).nextAfter(System.currentTimeMillis()), ZoneId.systemDefault().id, autoDelete, speed, format, downloadAllOnce)
        if (enabled && !downloadAllOnce) { scheduleChecks(id); checkNow(id) }
        else {
            work.cancelUniqueWork("yt-playlist-daily-$id")
            if (downloadAllOnce && !previous.downloadAllOnce) checkNow(id)
        }
    }

    suspend fun remove(id: String, deleteDownloads: Boolean): List<YtDownload> {
        // Remove membership first: a running check re-reads the subscription before enqueueing.
        val removed = detachYtPlaylist(db, id, deleteDownloads)
        work.cancelUniqueWork("yt-playlist-daily-$id")
        work.cancelUniqueWork("yt-playlist-now-$id")
        return removed
    }

    suspend fun sync(id: String, manual: Boolean = false, progress: suspend (String) -> Unit = {}) {
        if (!activeChecks.run(id) { syncInternal(id, manual, progress) }) {
            YtDownloadLog.info(0, null, "playlist.coalesced", "playlist=$id already checking")
        }
    }

    private suspend fun syncInternal(id: String, manual: Boolean, progress: suspend (String) -> Unit) {
        val subscription = db.subscriptions().get(id)?.takeIf { manual || it.enabled } ?: return
        val attempt = "playlist-${UUID.randomUUID()}"
        try {
            YtDownloadLog.info(0, attempt, "playlist.sync.start", "playlist=$id manual=$manual format=${subscription.format} ${gate.snapshot()} ${YtDownloadLog.network(context)}")
            progress("Waiting for downloader…")
            val result = gate.run(0, attempt, "playlist-$id") {
                progress("Loading playlist…")
                runInterruptible(Dispatchers.IO) {
                    YtDownloadLog.info(0, attempt, "playlist.runtime.init")
                    YoutubeDL.init(context)
                    updater.updateIfDue(YtDownload(videoId = id, kind = YtDownloadKind.AUDIO, attempt = attempt))
                    YtDownloadLog.info(0, attempt, "playlist.runtime.ready")
                    val request = YoutubeDLRequest("https://www.youtube.com/playlist?list=$id").apply {
                        addOption("--ignore-config")
                        addOption("--use-extractors", "youtube:tab,youtube")
                        addOption("--flat-playlist")
                        playlistScanLimit(subscription)?.let { addOption("--playlist-end", it) }
                        addOption("--dump-single-json")
                        addOption("--skip-download")
                        addOption("--socket-timeout", 30)
                        addOption("--retries", 3)
                    }
                    YtDownloadLog.info(0, attempt, "playlist.start", "playlist=$id scanLimit=${playlistScanLimit(subscription) ?: "all"}")
                    YoutubeDL.execute(request, attempt).let { response ->
                        YtDownloadLog.info(0, attempt, "playlist.extract.done", "exitCode=${response.exitCode} elapsedMs=${response.elapsedTime}")
                        if (response.err.isNotBlank()) YtDownloadLog.output(0, attempt, response.err)
                        JSONObject(response.out)
                    }
                }
            }
            val title = result.optString("title").takeIf { it.isNotBlank() } ?: subscription.title
            val array = result.optJSONArray("entries") ?: error("YouTube did not return playlist entries.")
            val fetched = (0 until array.length()).mapNotNull { index ->
                val entry = array.optJSONObject(index) ?: return@mapNotNull null
                val video = entry.optString("id")
                val name = entry.optString("title")
                if (YouTubeVideoUrl.parse("https://youtu.be/$video") == null || name.isBlank() ||
                    name in setOf("[Private video]", "[Deleted video]") || entry.optString("live_status") in setOf("is_live", "is_upcoming")) null
                else YtPlaylistEntry(id, video, name, index)
            }
            // Regex and database work stay off the UI thread. Re-read rules after the network request.
            progress(if (subscription.downloadAllOnce) "Queueing all matching episodes…" else "Selecting newest episodes…")
            var queued = 0
            db.withTransaction {
                val current = db.subscriptions().get(id)?.takeIf { manual || it.enabled } ?: return@withTransaction
                // Changed rules require a fresh bounded playlist fetch.
                if (current.titleRegex != subscription.titleRegex || current.downloadAllOnce != subscription.downloadAllOnce || current.latestCount != subscription.latestCount) {
                    db.subscriptions().checkMessage(id, "Settings changed during the check. Tap Check now again.")
                    return@withTransaction
                }
                artworkFromMetadata(result)?.let { db.subscriptions().setArtwork(id, it) }
                queued = enqueueLatestPlaylistEpisodes(db, current, fetched)
                db.subscriptions().checkMessage(id, if (queued > 0) "Check complete: $queued new download(s) queued." else "Check complete: no new matching downloads.")
                db.subscriptions().checked(id, title, System.currentTimeMillis(), null)
            }
            downloads.reconcile()
            downloads.scheduleBackgroundQueue()
            YtDownloadLog.info(0, attempt, "playlist.done", "playlist=$id scanned=${fetched.size}")
        } catch (cancelled: CancellationException) {
            YoutubeDL.destroyProcessById(attempt)
            throw cancelled
        } catch (error: Exception) {
            YtDownloadLog.failure(0, attempt, "playlist.failed", error)
            db.subscriptions().checked(id, subscription.title, System.currentTimeMillis(), youtubeDownloadError(error))
            throw error
        }
    }
}

@HiltWorker
class YtSubscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val subscriptions: YtSubscriptions,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("playlist") ?: return Result.failure()
        return try {
            if (inputData.getBoolean("manual", false)) subscriptions.sync(id, manual = true) { setProgress(workDataOf("message" to it)) }
            else subscriptions.runScheduled(id, runAttemptCount > 0) { setProgress(workDataOf("message" to it)) }
            Result.success()
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (!inputData.getBoolean("manual", false) && runAttemptCount < 2) Result.retry() else Result.failure() }
    }
}


/** Caller holds the Room transaction so selection and uniqueness checks are atomic. */
@Suppress("UNUSED_PARAMETER") // Legacy callers may supply dates; selection no longer uses them.
internal suspend fun enqueueLatestPlaylistEpisodes(
    db: YtDatabase, subscription: YtSubscription, fetched: List<YtPlaylistEntry>, dates: Map<String, Long> = emptyMap(),
): Int {
    val id = subscription.playlistId
    val dismissed = db.subscriptions().entries(id).filter { it.dismissed }.map { it.videoId to it.kind }.toSet()
    var queued = 0
    selectedPlaylistEpisodes(subscription, fetched).forEach { entry ->
        subscription.format.kinds.forEach { kind ->
            if ((entry.videoId to kind) !in dismissed) {
                db.subscriptions().addEntry(entry.copy(kind = kind))
                db.subscriptions().refreshEntry(id, entry.videoId, entry.title, entry.position, kind)
                if (db.downloads().find(entry.videoId, kind) == null) {
                    val inserted = db.downloads().insert(YtDownload(videoId = entry.videoId, kind = kind,
                        attempt = UUID.randomUUID().toString(), title = entry.title, standalone = false, background = true))
                    if (inserted != -1L) queued++
                }
            }
        }
    }
    return queued
}

internal fun subscriptionCheckStates(infos: List<WorkInfo>): Map<String, YtCheckState> =
infos.groupBy { info -> info.tags.firstOrNull { it.startsWith("playlist:") }?.removePrefix("playlist:").orEmpty() }
            .mapNotNull { (id, jobs) ->
                val active = jobs.firstOrNull { it.state == WorkInfo.State.RUNNING }
                    ?: jobs.firstOrNull { "manual" in it.tags && !it.state.isFinished }
                val latestManual = jobs.filter { "manual" in it.tags }.maxByOrNull { job ->
                    job.tags.firstOrNull { it.startsWith("requested:") }?.removePrefix("requested:")?.toLongOrNull() ?: 0L
                }
                val status = when {
                    active?.state == WorkInfo.State.RUNNING -> YtCheckState(active.progress.getString("message") ?: "Starting check…", true)
                    active != null -> YtCheckState("Check queued — waiting for network or Android…", true)
                    latestManual?.state == WorkInfo.State.FAILED -> YtCheckState("Check failed. See the error below or tap Check now to retry.", false)
                    else -> null
                }
                status?.let { id to it }
            }.toMap()
