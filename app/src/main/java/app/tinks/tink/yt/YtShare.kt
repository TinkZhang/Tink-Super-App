package app.tinks.tink.yt

import android.content.Intent

/** Shared text may include a title; only allowlisted YouTube links become form input. */
fun youtubeUrlFromSharedText(text: String?): String? {
    if (text == null) return null
    return Regex("https?://[^\\s<>\\\"]+", RegexOption.IGNORE_CASE).findAll(text.take(16_384)).firstNotNullOfOrNull { match ->
        val candidate = match.value.trimEnd('.', ',', ';', ')', ']', '}')
        YouTubeVideoUrl.parse(candidate)?.canonicalUrl
            ?: youtubePlaylistId(candidate)?.let { "https://www.youtube.com/playlist?list=$it" }
    }
}

fun youtubeUrlFromShare(intent: Intent?): String? {
    if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
    return youtubeUrlFromSharedText(intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString())
}
