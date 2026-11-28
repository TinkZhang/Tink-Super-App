package app.tinks.tink.yt

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class YtLabelsDatabaseTest {
    @Test fun labelsPersistAcrossReopenAndEditingDoesNotChangeDownloadOrSubscription() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val name = "yt-label-persistence.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, YtDatabase::class.java, name).build()
        var db = open()
        try {
            val id = db.downloads().insert(YtDownload(videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.AUDIO, attempt = "a", filePath = "/saved/audio.m4a")).toInt()
            val grouped = db.downloads().insert(YtDownload(videoId = "sO35ixeX0k8", kind = YtDownloadKind.AUDIO, attempt = "b", standalone = false)).toInt()
            assertEquals(1, db.downloads().setLabels(id, parseEpisodeLabels("History, Interviews").joinToString("\n")))
            assertEquals(0, db.downloads().setLabels(grouped, "Wrong group"))
            db.close(); db = open()
            assertEquals(listOf("History", "Interviews"), episodeLabels(db.downloads().get(id)!!))
            assertEquals("/saved/audio.m4a", db.downloads().get(id)!!.filePath)
            db.downloads().retry(id, "retry")
            assertEquals("History\nInterviews", db.downloads().get(id)!!.labels)
            db.downloads().setLabels(id, "")
            assertTrue(episodeLabels(db.downloads().get(id)!!).isEmpty())
            assertEquals(2, db.downloads().all().size)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
