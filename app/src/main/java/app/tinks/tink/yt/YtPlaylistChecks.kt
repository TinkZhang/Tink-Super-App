package app.tinks.tink.yt

import java.util.concurrent.ConcurrentHashMap

/** A manual check and cron tick must not scan the same playlist concurrently. */
internal class YtPlaylistChecks {
    private val active = ConcurrentHashMap.newKeySet<String>()
    suspend fun run(id: String, block: suspend () -> Unit): Boolean {
        if (!active.add(id)) return false
        try { block(); return true } finally { active.remove(id) }
    }
}
