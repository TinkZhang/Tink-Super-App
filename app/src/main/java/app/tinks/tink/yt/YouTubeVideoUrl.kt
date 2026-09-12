package app.tinks.tink.yt

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

data class YouTubeVideoUrl(val videoId: String) {
    val canonicalUrl: String get() = "https://www.youtube.com/watch?v=$videoId"

    companion object {
        private val videoIdPattern = Regex("[A-Za-z0-9_-]{11}")
        private val hosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com")

        fun parse(input: String): YouTubeVideoUrl? = runCatching {
            val uri = URI(input.trim())
            val host = uri.host?.lowercase(Locale.ROOT) ?: return null
            if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https") ||
                uri.userInfo != null || uri.port != -1
            ) return null
            val segments = uri.path.orEmpty().trim('/').split('/')
            val id = when {
                host == "youtu.be" && segments.size == 1 -> segments.single()
                host in hosts && uri.path == "/watch" -> {
                    val ids = uri.rawQuery.orEmpty().split('&').mapNotNull { part ->
                        val pair = part.split('=', limit = 2)
                        if (pair.size == 2 && pair[0] == "v") {
                            URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name())
                        } else null
                    }
                    ids.singleOrNull()
                }
                host in hosts && segments.size == 2 &&
                    segments[0] in setOf("shorts", "live", "embed") -> segments[1]
                else -> null
            }
            id?.takeIf(videoIdPattern::matches)?.let(::YouTubeVideoUrl)
        }.getOrNull()
    }
}
