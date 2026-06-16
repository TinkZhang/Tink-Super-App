package app.tinks.tink.geography

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class GeographyDataTest {

    @Test
    fun filterByCountryName_matchesChineseEnglishAndLocalNames() {
        val countries = assetCountries()

        assertEquals("日本", countries.filterByCountryName("Japan").single().chineseShortName)
        assertEquals("巴西", countries.filterByCountryName("Brasil").single().chineseShortName)
        assertTrue(countries.filterByCountryName("中华人民共和国").any { it.chineseShortName == "中国" })
    }

    @Test
    fun assetCountries_containsWorldScaleCountryData() {
        val countries = assetCountries()

        assertTrue(countries.size >= 200)
        assertTrue(countries.all { it.flagEmoji.isNotBlank() })
        assertTrue(countries.all { it.cca2.length == 2 })
    }

    @Test
    fun buildGeographyQuestion_returnsFourOptionsContainingAnswer() {
        val question = buildGeographyQuestion(
            countries = assetCountries(),
            type = GeographyQuizType.CountryToMap,
            random = Random(7),
        )

        assertEquals(GeographyQuizType.CountryToMap, question.type)
        assertEquals(4, question.options.size)
        assertTrue(question.options.any { it.id == question.answer.id })
        assertEquals(4, question.options.map { it.id }.distinct().size)
    }

    private fun assetCountries(): List<GeographyCountry> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return GeographyData.loadCountries(context)
    }
}
