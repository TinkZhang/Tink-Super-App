package app.tinks.tink.yt

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtMediaFiles @Inject constructor(@ApplicationContext context: Context) {
    private val root = File(context.filesDir, "yt")

    fun directory(id: Int): File {
        require(id > 0)
        return File(root, id.toString())
    }

    fun completedFile(item: YtDownload): File? {
        val file = item.filePath?.let(::File)?.canonicalFile ?: return null
        return file.takeIf {
            it.parentFile == directory(item.id).canonicalFile &&
                it.isFile && it.length() > 0 && it.extension in setOf("mp4", "m4a")
        }
    }
}
