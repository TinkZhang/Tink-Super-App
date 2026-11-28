package app.tinks.tink.yt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YtDownloadLogTest {
    @Test fun preservesFailureDetailsAndStackTrace() {
        val text = "YoutubeDLException: ERROR: HTTP Error 403: Forbidden\n\tat downloader.execute(Downloader.kt:42)"
        assertEquals(text, sanitizeYtLog(text))
    }

    @Test fun redactsSignedUrlsAndAuthenticationDiagnostics() {
        val text = """
            [debug] Request https://media.example/video?signature=secret
            Authorization: Bearer private-token
            Cookie: session=private-session
            [debug] Proxy map: {'https': 'socks5://user:password@proxy:1080'}
            ERROR: HTTP Error 403: Forbidden
        """.trimIndent()
        val result = sanitizeYtLog(text)
        listOf("signature", "private-token", "private-session", "password", "media.example").forEach {
            assertFalse(result.contains(it))
        }
        assertTrue(result.contains("[URL redacted]"))
        assertTrue(result.contains("ERROR: HTTP Error 403: Forbidden"))
    }

    @Test fun stripsTerminalColorsWithoutLosingErrorText() {
        assertEquals("ERROR: unavailable", sanitizeYtLog("\u001b[31mERROR:\u001b[0m unavailable"))
    }
}
