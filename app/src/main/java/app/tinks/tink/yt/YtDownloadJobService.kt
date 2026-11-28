package app.tinks.tink.yt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import androidx.core.app.NotificationCompat
import app.tinks.tink.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

internal const val YT_JOB_NAMESPACE = "yt-downloads"
internal const val YT_ATTEMPT = "attempt"
private const val CHANNEL = "yt_downloads"

@Singleton
class AndroidYtDownloadScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : YtDownloadScheduler {
    private val scheduler get() = context.getSystemService(JobScheduler::class.java).forNamespace(YT_JOB_NAMESPACE)

    override fun schedule(download: YtDownload): Boolean {
        YtDownloadLog.info(download.id, download.attempt, "schedule.start", YtDownloadLog.network(context))
        val extras = PersistableBundle().apply { putString(YT_ATTEMPT, download.attempt) }
        val result = scheduler.schedule(
            JobInfo.Builder(download.id, ComponentName(context, YtDownloadJobService::class.java))
                .setUserInitiated(!download.background)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresStorageNotLow(true)
                .setExtras(extras)
                .build()
        )
        YtDownloadLog.info(download.id, download.attempt, "schedule.result", "result=$result success=${result == JobScheduler.RESULT_SUCCESS}")
        return result == JobScheduler.RESULT_SUCCESS
    }

    override fun cancel(id: Int) = scheduler.cancel(id)

    override fun pendingAttempts(): Map<Int, String> = scheduler.allPendingJobs.mapNotNull { job ->
        job.extras.getString(YT_ATTEMPT)?.let { job.id to it }
    }.toMap()
}

@AndroidEntryPoint
class YtDownloadJobService : JobService() {
    @Inject lateinit var runner: YtDownloadRunner
    @Inject lateinit var repository: YtDownloadRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val running = ConcurrentHashMap<String, Job>()

    override fun onStartJob(params: JobParameters): Boolean {
        val attempt = params.extras.getString(YT_ATTEMPT) ?: return false
        YtDownloadLog.info(params.jobId, attempt, "job.start",
            "userInitiated=${params.isUserInitiatedJob} assignedNetwork=${params.network != null} ${YtDownloadLog.network(this)}")
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "YT downloads", NotificationManager.IMPORTANCE_LOW)
        )
        setNotification(params, notificationId(params.jobId), notification(params.jobId, 0, false),
            JOB_END_NOTIFICATION_POLICY_REMOVE)
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                runner.run(params.jobId, attempt) { progress, processing ->
                    getSystemService(NotificationManager::class.java).notify(
                        notificationId(params.jobId), notification(params.jobId, progress, processing))
                }
                YtDownloadLog.info(params.jobId, attempt, "job.finished")
                jobFinished(params, false)
                repository.scheduleBackgroundQueue()
            } finally {
                running.remove(attempt)
            }
        }
        running[attempt] = job
        job.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        YtDownloadLog.info(params.jobId, params.extras.getString(YT_ATTEMPT), "job.stopped",
            "stopReason=${params.stopReason} reschedule=${params.stopReason != JobParameters.STOP_REASON_USER} ${YtDownloadLog.network(this)}")
        params.extras.getString(YT_ATTEMPT)?.let { attempt ->
            running.remove(attempt)?.cancel()
        }
        // Android reschedules system interruptions; explicit cancellation removes the job.
        return params.stopReason != JobParameters.STOP_REASON_USER
    }

    override fun onDestroy() {
        running.keys.forEach { YtDownloadLog.info(-1, it, "service.destroyed", "active job cancelled") }
        scope.cancel()
        super.onDestroy()
    }

    private fun notification(id: Int, progress: Int, processing: Boolean): Notification {
        val open = PendingIntent.getActivity(this, id, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cancel = PendingIntent.getBroadcast(this, id,
            Intent(this, YtCancelDownloadReceiver::class.java).putExtra("download_id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("YT download")
            .setContentText(if (processing) "Preparing your file…" else "Downloading · $progress%")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, processing || progress == 0)
            .addAction(0, "Cancel", cancel)
            .build()
    }

    private fun notificationId(id: Int) = 1_000_000 + id
}

@AndroidEntryPoint
class YtCancelDownloadReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: YtDownloadRepository

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("download_id", -1)
        if (id <= 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { repository.cancel(id) } finally { pending.finish() }
        }
    }
}
