package app.tinks.tink.sync

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor() {
    fun startObservingNetwork() = Unit

    fun initialSync() = Unit
}
