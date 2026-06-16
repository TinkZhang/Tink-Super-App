package app.tinks.tink.testing

import android.app.Application
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class ComposeCounterTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun counter_updatesText_afterClick() {
        composeRule.setContent {
            CounterButton()
        }

        composeRule.onNodeWithText("Count: 0").assertIsDisplayed()
        composeRule.onNodeWithText("Increment").performClick()
        composeRule.onNodeWithText("Count: 1").assertIsDisplayed()
    }
}

@Composable
private fun CounterButton() {
    var count by remember { mutableIntStateOf(0) }

    Button(onClick = { count += 1 }) {
        Text("Increment")
        Text("Count: $count")
    }
}
