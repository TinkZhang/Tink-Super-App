package app.tinks.tink.yt

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONObject
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class YtDownloadResult(val title: String, val file: File)

interface YtDownloadEngine {
    suspend fun download(item: YtDownload, onProgress: (Int, Boolean) -> Unit): YtDownloadResult
    fun cancel(attempt: String)
}

/** All input becomes a canonical single-video URL; no user-provided downloader options. */
fun youtubeDownloadRequest(item: YtDownload, directory: File): YoutubeDLRequest {
    val url = requireNotNull(YouTubeVideoUrl.parse("https://youtu.be/${item.videoId}"))
    return YoutubeDLRequest(url.canonicalUrl).apply {
        addOption("--ignore-config")
        addOption("--no-playlist")
        addOption("--use-extractors", "youtube")
        addOption("--no-mtime")
        addOption("--continue")
        addOption("--newline")
        addOption("--socket-timeout", 30)
        addOption("--retries", 3)
        addOption("--fragment-retries", 3)
        addOption("--write-info-json")
        addOption("--no-write-playlist-metafiles")
        addOption("--match-filters", "!is_live")
        addOption("-o", File(directory, "media.%(ext)s").absolutePath)
        addCommands(listOf("--print-to-file", "after_move:%(filepath)s", File(directory, "completed.txt").absolutePath))
        when (item.kind) {
            YtDownloadKind.AUDIO -> {
                addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
                addOption("--extract-audio")
                addOption("--audio-format", "m4a")
            }
            YtDownloadKind.VIDEO -> {
                addOption("-f", "bv[height<=720][ext=mp4]+ba[ext=m4a]/b[height<=720][ext=mp4]/bv[height<=720]+ba/b[height<=720]")
                addOption("--merge-output-format", "mp4")
                addOption("--remux-video", "mp4")
            }
        }
    }
}

@Singleton
class YoutubeDlEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val files: YtMediaFiles,
) : YtDownloadEngine {
    override suspend fun download(item: YtDownload, onProgress: (Int, Boolean) -> Unit): YtDownloadResult =
        runInterruptible(Dispatchers.IO) {
            YoutubeDL.init(context)
            FFmpeg.getInstance().init(context)
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            val directory = files.directory(item.id)
            check(directory.isDirectory || directory.mkdirs()) { "Cannot create download folder." }
            if (directory.usableSpace < 100L * 1024 * 1024) throw IOException("Not enough storage space.")
            val completion = File(directory, "completed.txt")
            completion.delete()
            var lastProgress = -1
            var wasProcessing = false
            YoutubeDL.execute(youtubeDownloadRequest(item, directory), item.attempt) { progress, _, line ->
                val processing = line.startsWith("[Merger]") || line.startsWith("[ExtractAudio]") ||
                    line.startsWith("[VideoRemuxer]")
                val value = if (progress.isFinite()) progress.toInt().coerceIn(0, 99) else 0
                if (value != lastProgress || processing != wasProcessing) {
                    onProgress(value, processing)
                    lastProgress = value
                    wasProcessing = processing
                }
            }
            val path = completion.takeIf(File::isFile)?.readLines()?.lastOrNull { it.isNotBlank() }
                ?: throw IOException("YouTube did not produce a downloadable file. Live streams are not supported.")
            val file = files.completedFile(item.copy(filePath = path))
                ?: throw IOException("The downloaded media file could not be verified.")
            val metadata = File(directory, "media.info.json")
            val title = runCatching { JSONObject(metadata.readText()).optString("title") }.getOrNull()
                ?.takeIf(String::isNotBlank) ?: item.videoId
            // Sidecars can contain short-lived URLs. Only the local media and Room metadata are needed.
            metadata.delete()
            completion.delete()
            YtDownloadResult(title, file)
        }

    override fun cancel(attempt: String) {
        YoutubeDL.destroyProcessById(attempt)
    }
}

fun youtubeDownloadError(error: Throwable): String {
    val message = error.message.orEmpty().lowercase()
    return when {
        "space" in message || "enospc" in message -> "Not enough storage. Free some space and tap Retry."
        "sign in" in message || "bot" in message || "403" in message ->
            "YouTube blocked this download. Try again later or use another video."
        "private" in message || "unavailable" in message || "removed" in message || "members-only" in message ->
            "This video is unavailable or requires account access. Try a public YouTube video."
        "live" in message -> "Live streams are not supported. Try a finished YouTube video."
        "timed out" in message || "network" in message || "resolve" in message || "connection" in message ->
            "Could not connect to YouTube. Check your network and tap Retry."
        "requested format" in message -> "YouTube did not offer a supported format. Try another video."
        else -> "Download failed. Check your connection and tap Retry."
    }
}
