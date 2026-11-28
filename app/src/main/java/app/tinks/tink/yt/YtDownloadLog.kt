package app.tinks.tink.yt

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import app.tinks.tink.BuildConfig

/** Debug-only diagnostics. Every chunk retains the job/attempt so concurrent logs stay attributable. */
internal object YtDownloadLog {
    const val TAG = "YTDownload"

    fun info(id: Int, attempt: String?, event: String, details: String = "") =
        write(Log.INFO, id, attempt, event, details)

    fun output(id: Int, attempt: String, line: String) =
        write(Log.DEBUG, id, attempt, "yt-dlp.output", line)

    fun failure(id: Int, attempt: String?, event: String, error: Throwable) {
        if (BuildConfig.DEBUG) write(Log.ERROR, id, attempt, event, error.stackTraceToString())
    }

    private fun write(level: Int, id: Int, attempt: String?, event: String, details: String) {
        if (!BuildConfig.DEBUG) return
        val prefix = "download=$id attempt=${attempt ?: "none"} $event"
        // Split below Logcat's per-entry limit; keep the full exception, not just its first line.
        sanitizeYtLog(details).ifEmpty { "-" }.chunked(900).forEach { chunk ->
            Log.println(level, TAG, "$prefix | $chunk")
        }
    }

    fun network(context: Context): String {
        if (!BuildConfig.DEBUG) return ""
        return runCatching {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val caps = manager.getNetworkCapabilities(manager.activeNetwork)
            "networkPresent=${caps != null} validated=${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)} " +
                "wifi=${caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)} " +
                "cellular=${caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)} " +
                "vpn=${caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)} metered=${manager.isActiveNetworkMetered}"
        }.getOrElse { "networkSnapshotUnavailable=${it.javaClass.simpleName}" }
    }
}

/** Avoid sharing signed media URLs, authentication headers or proxy credentials in bug reports. */
internal fun sanitizeYtLog(text: String): String = text
    .replace(Regex("\\u001B\\[[0-?]*[ -/]*[@-~]"), "")
    .replace(Regex("(?im)^.*(?:authorization|cookie|proxy map).*$"), "[sensitive diagnostic line redacted]")
    .replace(Regex("(?i)https?://[^\\s\\\"'<>]+"), "[URL redacted]")
