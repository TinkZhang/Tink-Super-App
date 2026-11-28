package app.tinks.tink.yt

import android.content.Context
import android.os.SystemClock
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val preferences by lazy { context.getSharedPreferences("yt-runtime", Context.MODE_PRIVATE) }
    private val check by lazy {
        YtDlpUpdateCheck(
            lastSuccess = { preferences.getLong("last-successful-check", 0L) },
            saveSuccess = { preferences.edit().putLong("last-successful-check", it).apply() },
        )
    }

    /** Called on IO after init, under the runner's native-work lock, before starting yt-dlp. */
    fun updateIfDue(item: YtDownload) {
        val started = SystemClock.elapsedRealtime()
        try {
            val checked = check.run(hasInstalledVersion = YoutubeDL.version(context) != null) {
                YtDownloadLog.info(item.id, item.attempt, "update.start",
                    "channel=stable previousVersion=${YoutubeDL.version(context) ?: "bundled"}")
                val result = YoutubeDL.updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
                checkNotNull(result) { "yt-dlp update returned no status" }
                YtDownloadLog.info(item.id, item.attempt, "update.done",
                    "status=$result version=${YoutubeDL.version(context)} elapsedMs=${SystemClock.elapsedRealtime() - started}")
            }
            if (!checked) YtDownloadLog.info(item.id, item.attempt, "update.skipped",
                "successful check within 24 hours version=${YoutubeDL.version(context)}")
        } catch (error: Exception) {
            if (error is CancellationException || error is InterruptedException) throw error
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            // A GitHub outage must not prevent trying the already installed downloader.
            // Failed checks are not cached, so Retry can attempt the update again.
            YtDownloadLog.failure(item.id, item.attempt, "update.failed", error)
            YtDownloadLog.info(item.id, item.attempt, "update.fallback",
                "usingVersion=${YoutubeDL.version(context) ?: "bundled"}")
        }
    }
}

internal class YtDlpUpdateCheck(
    private val lastSuccess: () -> Long,
    private val saveSuccess: (Long) -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun run(hasInstalledVersion: Boolean, update: () -> Unit): Boolean {
        val current = now()
        val previous = lastSuccess()
        if (hasInstalledVersion && previous > 0 && current >= previous && current - previous < DAY_MS) return false
        if (Thread.currentThread().isInterrupted) throw InterruptedException()
        update()
        if (Thread.currentThread().isInterrupted) throw InterruptedException()
        saveSuccess(now())
        return true
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
