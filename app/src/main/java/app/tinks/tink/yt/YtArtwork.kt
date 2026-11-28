package app.tinks.tink.yt

import org.json.JSONObject
import java.io.File
import java.net.URI
import java.net.HttpURLConnection
import java.net.URL

fun youtubeThumbnail(videoId: String): String? = videoId.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
    ?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }

fun youtubeArtworkUrl(value: String?): String? = value?.takeIf {
    runCatching {
        val uri = URI(it)
        val host = uri.host?.lowercase().orEmpty()
        uri.scheme == "https" && uri.userInfo == null && uri.port == -1 &&
            listOf("ytimg.com", "ggpht.com", "googleusercontent.com").any { domain -> host == domain || host.endsWith(".$domain") }
    }.getOrDefault(false)
}

fun artworkFromMetadata(metadata: JSONObject): String? {
    youtubeArtworkUrl(metadata.optString("thumbnail"))?.let { return it }
    val thumbnails = metadata.optJSONArray("thumbnails") ?: return null
    return (thumbnails.length() - 1 downTo 0).firstNotNullOfOrNull { index ->
        youtubeArtworkUrl(thumbnails.optJSONObject(index)?.optString("url"))
    }
}

fun episodeArtwork(item: YtDownload): String? {
    val cover = item.filePath?.let { File(File(it).parentFile, "cover.jpg") }
    return if (cover?.isFile == true) cover.toURI().toString() else youtubeThumbnail(item.videoId)
}

/** Optional bounded fetch; artwork failure never invalidates a completed media download. */
fun saveEpisodeArtwork(directory: File, source: String?) {
    val url = youtubeArtworkUrl(source) ?: return
    val connection = URL(url).openConnection() as HttpURLConnection
    val temporary = File(directory, "cover.tmp")
    try {
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.instanceFollowRedirects = false
        check(connection.responseCode == 200) { "Artwork HTTP ${connection.responseCode}" }
        check(connection.contentType.orEmpty().startsWith("image/")) { "Artwork response was not an image" }
        connection.inputStream.use { input -> temporary.outputStream().use { output ->
            val bytes = ByteArray(8192)
            var total = 0
            while (true) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException()
                val count = input.read(bytes)
                if (count < 0) break
                total += count
                check(total <= 3_000_000) { "Artwork too large" }
                output.write(bytes, 0, count)
            }
        } }
        check(temporary.length() > 0 && temporary.renameTo(File(directory, "cover.jpg"))) { "Could not save artwork" }
    } finally { connection.disconnect(); temporary.delete() }
}
