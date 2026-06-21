package app.tinks.tink.sync

import app.tinks.tink.home.HomeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val homeRepository: HomeRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startObservingNetwork() = Unit

    fun initialSync() {
        scope.launch {
            homeRepository.syncPendingActions()
        }
    }
}
