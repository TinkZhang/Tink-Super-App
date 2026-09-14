package app.tinks.tink.random

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class RandomRepositoryTest {
    @Test fun settingsSurviveRepositoryRecreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = RandomSettings(type = RandomType.Wheel, diceCount = 5, wheelName = "去哪儿", sections = listOf(WheelSection("公园", 3), WheelSection("博物馆", 7)), sound = false, speech = false)
        RandomRepository(context).save(config)
        assertEquals(config, RandomRepository(context).settings.value)
    }
    @Test fun malformedOrObsoleteStorage_fallsBackToValidDefaults() {
        listOf(null, "", "not json", "{}", RandomRepository.encode(RandomSettings(diceCount = 99))).forEach {
            assertEquals(RandomSettings(), RandomRepository.decode(it))
        }
    }
}
