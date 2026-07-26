package app.tinks.tink

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.tinks.tink.sync.SyncManager
import app.tinks.tink.sync.WeightSyncWorker
import app.tinks.tink.book.ReadKeeperSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var readKeeperSyncScheduler: ReadKeeperSyncScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // 启动网络监听与同步
        syncManager.startObservingNetwork()
        syncManager.initialSync()
        schedulePeriodicSync()
        readKeeperSyncScheduler.schedulePeriodic()
    }

    private fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<WeightSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weight_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
