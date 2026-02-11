package app.tinks.tink.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.tinks.tink.haircut.HaircutRepository
import app.tinks.tink.weight.WeightRepository
import app.tinks.tink.zi.ZiRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WeightSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val weightRepository: WeightRepository,
    private val ziRepository: ZiRepository,
    private val haircutRepository: HaircutRepository,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return try {
//            weightRepository.syncPending()
//            ziRepository.syncPending()
//            haircutRepository.syncPending()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}