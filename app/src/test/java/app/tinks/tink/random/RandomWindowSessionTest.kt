package app.tinks.tink.random

import android.app.Activity
import android.app.Application
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class RandomWindowSessionTest {
    @Test fun keepsScreenOnOnlyDuringSession_andRestoresExistingFlag() {
        Robolectric.buildActivity(Activity::class.java).setup().use { activity ->
            val window = activity.get().window
            window.clearFlags(FLAG_KEEP_SCREEN_ON)
            val session = RandomWindowSession(window)
            repeat(2) {
                session.enter(); session.enter()
                assertTrue(window.attributes.flags and FLAG_KEEP_SCREEN_ON != 0)
                session.leave(); session.leave()
                assertEquals(0, window.attributes.flags and FLAG_KEEP_SCREEN_ON)
            }
            window.addFlags(FLAG_KEEP_SCREEN_ON)
            session.enter(); session.leave()
            assertTrue(window.attributes.flags and FLAG_KEEP_SCREEN_ON != 0)
        }
    }
}
