package app.tinks.tink.yt

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class YtVideoFilesTest {
    @Test fun requiresNonemptyFileInsideTheDownloadsOwnDirectory() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val files = YtMediaFiles(context)
        val folder = files.directory(99).apply { mkdirs() }
        val outside = File(context.cacheDir, "outside.mp4").apply { writeText("fixture") }
        try {
            val item = YtDownload(99, "LbCHAQ7HKu4", YtDownloadKind.VIDEO, "a", status = YtDownloadStatus.READY)
            assertNull(files.completedFile(item.copy(filePath = outside.absolutePath)))
            val video = File(folder, "media.mp4").apply { writeText("fixture") }
            assertEquals(video.canonicalFile, files.completedFile(item.copy(filePath = video.absolutePath)))
            video.writeText("")
            assertNull(files.completedFile(item.copy(filePath = video.absolutePath)))
            video.delete()
            assertNull(files.completedFile(item.copy(filePath = video.absolutePath)))
        } finally { folder.deleteRecursively(); outside.delete() }
    }
}
