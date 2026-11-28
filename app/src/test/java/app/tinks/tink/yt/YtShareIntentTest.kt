package app.tinks.tink.yt

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class YtShareIntentTest {
    @Test fun readsOnlySinglePlainTextShares() {
        val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "https://youtu.be/LbCHAQ7HKu4")
        assertNotNull(youtubeUrlFromShare(share))
        assertNull(youtubeUrlFromShare(Intent(share).setAction(Intent.ACTION_VIEW)))
        assertNull(youtubeUrlFromShare(Intent(share).setAction(Intent.ACTION_SEND_MULTIPLE)))
        assertNull(youtubeUrlFromShare(Intent(share).setType("image/jpeg")))
        assertNull(youtubeUrlFromShare(Intent(Intent.ACTION_SEND).setType("text/plain")))
    }
    @Test fun manifestExposesTinkAsATextShareTarget() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val share = Intent(Intent.ACTION_SEND).setType("text/plain").addCategory(Intent.CATEGORY_DEFAULT).setPackage(context.packageName)
        val targets = context.packageManager.queryIntentActivities(share, 0)
        assertTrue(targets.any { it.activityInfo.name == "app.tinks.tink.MainActivity" && it.activityInfo.exported })
    }
}
