package app.tinks.tink

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import app.tinks.tink.ui.theme.TinkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var quickAddRequestId by mutableIntStateOf(0)
    private var openAddFromQuickSettings by mutableStateOf(false)
    private var readKeeperStopRequestId by mutableIntStateOf(0)
    private var openReadKeeperFromNotification by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleLaunchIntent(intent)
        enableEdgeToEdge()
        setContent {
            TinkTheme() {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 启动主应用结构
                    MyApp(
                        quickAddRequestId = quickAddRequestId,
                        openAddFromQuickSettings = openAddFromQuickSettings,
                        onQuickSettingsRequestConsumed = {
                            openAddFromQuickSettings = false
                        },
                        readKeeperStopRequestId = readKeeperStopRequestId,
                        openReadKeeperFromNotification = openReadKeeperFromNotification,
                        onReadKeeperNotificationRequestConsumed = {
                            openReadKeeperFromNotification = false
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_ADD_TIME_ENTRY_FROM_TILE -> {
                quickAddRequestId += 1
                openAddFromQuickSettings = true
                intent.action = null
            }
            ACTION_STOP_READKEEPER_SESSION,
            ACTION_OPEN_READKEEPER_SESSION -> {
                if (intent.action == ACTION_STOP_READKEEPER_SESSION) {
                    readKeeperStopRequestId += 1
                }
                openReadKeeperFromNotification = true
                intent.action = null
            }
        }
    }

    companion object {
        const val ACTION_ADD_TIME_ENTRY_FROM_TILE = "app.tinks.tink.action.ADD_TIME_ENTRY_FROM_TILE"
        const val ACTION_OPEN_READKEEPER_SESSION = "app.tinks.tink.action.OPEN_READKEEPER_SESSION"
        const val ACTION_STOP_READKEEPER_SESSION = "app.tinks.tink.action.STOP_READKEEPER_SESSION"
    }
}

@Preview(showBackground = true)
@Composable
fun MyAppPreview() {
    TinkTheme() {
        MyApp()
    }
}
