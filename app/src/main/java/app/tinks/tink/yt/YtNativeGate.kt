package app.tinks.tink.yt

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/** Serializes native operations and identifies the owner when another operation is waiting. */
@Singleton
class YtNativeGate @Inject constructor() {
    private val mutex = Mutex()
    private data class Owner(val name: String, val since: Long)
    @Volatile private var owner: Owner? = null
    fun snapshot(): String = owner?.let { "owner=${it.name} heldMs=${(System.nanoTime() - it.since) / 1_000_000}" } ?: "owner=none"

    suspend fun <T> run(id: Int, attempt: String?, operation: String, block: suspend () -> T): T = coroutineScope {
        val start = System.nanoTime()
        var acquired = false
        YtDownloadLog.info(id, attempt, "gate.wait", "operation=$operation ${snapshot()}")
        val heartbeat = launch {
            while (isActive) {
                delay(15_000)
                YtDownloadLog.info(id, attempt, if (acquired) "gate.holding" else "gate.waiting",
                    "operation=$operation elapsedMs=${(System.nanoTime() - start) / 1_000_000} ${snapshot()}")
            }
        }
        try {
            mutex.lock()
            acquired = true
            owner = Owner("$operation download=$id attempt=$attempt", System.nanoTime())
            YtDownloadLog.info(id, attempt, "gate.acquired", "operation=$operation waitedMs=${(System.nanoTime() - start) / 1_000_000}")
            try { block() }
            finally {
                YtDownloadLog.info(id, attempt, "gate.released", "operation=$operation ${snapshot()}")
                owner = null
                mutex.unlock()
            }
        } catch (cancelled: CancellationException) {
            YtDownloadLog.info(id, attempt, "gate.cancelled", "operation=$operation acquired=$acquired ${snapshot()}")
            throw cancelled
        } finally { heartbeat.cancel() }
    }
}
