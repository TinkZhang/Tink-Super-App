package app.tinks.tink.secureweb

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import app.tinks.tink.ui.theme.TinkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class SecureWebScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screen_requestsAuthenticationOnLaunch() {
        val authenticator = RecordingAuthenticator()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                SecureWebScreen(authenticator = authenticator)
            }
        }

        composeRule.waitForIdle()

        assertEquals(1, authenticator.requestCount)
        composeRule.onNodeWithTag("secure_web_locked_content").assertIsDisplayed()
        composeRule.onNodeWithTag("secure_web_auth_progress").assertIsDisplayed()
    }

    @Test
    fun screen_showsWebContentAfterSuccessfulAuthentication() {
        val authenticator = RecordingAuthenticator()

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                SecureWebScreen(
                    authenticator = authenticator,
                    webContent = { url ->
                        Text(url, Modifier.testTag("secure_web_fake_content"))
                    },
                )
            }
        }

        composeRule.runOnIdle {
            authenticator.succeedLatest()
        }

        composeRule.onNodeWithTag("secure_web_fake_content").assertIsDisplayed()
        composeRule.onNodeWithText(SECURE_WEB_URL).assertIsDisplayed()
    }

    @Test
    fun screen_requestsAuthenticationAgainWhenLifecycleResumes() {
        val authenticator = RecordingAuthenticator()
        val lifecycleOwner = TestLifecycleOwner().apply {
            handle(Lifecycle.Event.ON_CREATE)
            handle(Lifecycle.Event.ON_START)
            handle(Lifecycle.Event.ON_RESUME)
        }

        composeRule.setContent {
            TinkTheme(dynamicColor = false) {
                SecureWebScreen(
                    lifecycleOwner = lifecycleOwner,
                    authenticator = authenticator,
                    webContent = { url ->
                        Text(url, Modifier.testTag("secure_web_fake_content"))
                    },
                )
            }
        }

        composeRule.runOnIdle {
            authenticator.succeedLatest()
        }
        composeRule.onNodeWithTag("secure_web_fake_content").assertIsDisplayed()

        composeRule.runOnIdle {
            lifecycleOwner.handle(Lifecycle.Event.ON_PAUSE)
            lifecycleOwner.handle(Lifecycle.Event.ON_RESUME)
        }

        assertEquals(2, authenticator.requestCount)
        composeRule.onAllNodesWithTag("secure_web_fake_content").assertCountEquals(0)
        composeRule.onNodeWithTag("secure_web_auth_progress").assertIsDisplayed()

        composeRule.runOnIdle {
            authenticator.succeedLatest()
        }

        composeRule.onNodeWithTag("secure_web_fake_content").assertIsDisplayed()
    }
}

private class RecordingAuthenticator : SecureWebAuthenticator {
    private val requests = mutableListOf<AuthRequest>()

    val requestCount: Int
        get() = requests.size

    override fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        requests.add(AuthRequest(onSuccess, onError))
    }

    fun succeedLatest() {
        requests.last().onSuccess()
    }
}

private data class AuthRequest(
    val onSuccess: () -> Unit,
    val onError: (String) -> Unit,
)

private class TestLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = registry

    fun handle(event: Lifecycle.Event) {
        registry.handleLifecycleEvent(event)
    }
}
