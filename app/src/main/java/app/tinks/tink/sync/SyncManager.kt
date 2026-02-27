package app.tinks.tink.sync

import app.tinks.tink.haircut.HaircutRepository
import app.tinks.tink.weight.WeightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val weightRepository: WeightRepository,
    private val haircutRepository: HaircutRepository,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startObservingNetwork() {
        scope.launch {
            networkMonitor.isConnected.collectLatest { connected ->
                if (connected) {
                    try {
                        weightRepository.syncPending()
                        haircutRepository.syncPending()
                        weightRepository.refreshFromRemote()
                        haircutRepository.refreshFromRemote()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * 启动时同步一次
     */
    fun initialSync() {
        scope.launch {
            try {
                weightRepository.syncPending()
                haircutRepository.syncPending()
                weightRepository.refreshFromRemote()
                haircutRepository.refreshFromRemote()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}