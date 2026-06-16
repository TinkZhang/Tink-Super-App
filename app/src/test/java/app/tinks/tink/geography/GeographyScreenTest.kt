package app.tinks.tink.geography

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import app.tinks.tink.ui.theme.TinkTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class GeographyScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun browseSearch_opensCountryDetailAndFullScreenImage() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                GeographyScreen(
                    countries = assetCountries(),
                    enableFeedback = false,
                )
            }
        }

        composeRule.onNodeWithTag("geography_search_field").performTextInput("Japan")
        composeRule.onNodeWithText("日本").assertIsDisplayed()
        composeRule.onNodeWithTag("geography_country_jpn").performClick()

        composeRule.onNodeWithTag("geography_detail_jpn").assertIsDisplayed()
        composeRule.onNodeWithText("日本国").assertIsDisplayed()

        composeRule.onNodeWithTag("geography_detail_jpn")
            .performScrollToNode(hasTestTag("geography_detail_flag_jpn"))
        composeRule.onNodeWithTag("geography_detail_flag_jpn").performClick()
        composeRule.onNodeWithTag("geography_image_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("geography_image_close").performClick()
        composeRule.onNodeWithTag("geography_detail_jpn").assertIsDisplayed()
    }

    @Test
    fun quizMode_switchesQuestionTypes() {
        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                GeographyScreen(
                    countries = assetCountries(),
                    enableFeedback = false,
                )
            }
        }

        composeRule.onNodeWithTag("geography_mode_quiz").performClick()
        composeRule.onNodeWithTag("geography_quiz_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("geography_quiz_type_MapToCountry").performClick()
        composeRule.onNodeWithText("地图上标出的是哪个国家？").assertIsDisplayed()
        composeRule.onNodeWithTag("geography_prompt_map").assertIsDisplayed()
    }

    private fun assetCountries(): List<GeographyCountry> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return GeographyData.loadCountries(context)
    }
}
