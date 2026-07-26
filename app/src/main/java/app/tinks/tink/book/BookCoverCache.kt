package app.tinks.tink.book

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookCoverCache @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val cacheDirectory = File(context.filesDir, "readkeeper-covers").apply { mkdirs() }
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cacheMutex = Mutex()

    fun cacheInBackground(
        url: String?,
        onComplete: suspend () -> Unit = {},
    ) {
        val coverUrl = url?.takeIf { it.isNotBlank() } ?: return
        cacheScope.launch {
            val cached = runCatching {
                cacheMutex.withLock { cache(coverUrl) }
            }.isSuccess
            if (cached) runCatching { onComplete() }
        }
    }

    private suspend fun cache(coverUrl: String): Unit = withContext(Dispatchers.IO) {
        val target = fileFor(coverUrl)
        if (target.exists() && target.length() > 0) return@withContext

        val temporary = File(target.parentFile, "${target.name}.tmp")
        val connection = (URL(coverUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext
            connection.inputStream.use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
        } finally {
            connection.disconnect()
            temporary.delete()
        }
    }

    fun bitmap(url: String?): Bitmap? {
        val coverUrl = url?.takeIf { it.isNotBlank() } ?: return null
        val file = fileFor(coverUrl).takeIf { it.exists() && it.length() > 0 } ?: return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    private fun fileFor(url: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
        return File(cacheDirectory, "$digest.img")
    }
}
