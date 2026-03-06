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
import androidx.compose.ui.tooling.preview.Preview
import app.tinks.tink.ui.theme.TinkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var quickAddRequestId by mutableIntStateOf(0)
    private var openAddFromQuickSettings by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleLaunchIntent(intent)
        enableEdgeToEdge()
        setContent {
            TinkTheme() {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 启动主应用结构
                    MyApp(
                        quickAddRequestId = quickAddRequestId,
                        openAddFromQuickSettings = openAddFromQuickSettings,
                        onQuickSettingsRequestConsumed = {
                            openAddFromQuickSettings = false
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
        if (intent?.action == ACTION_ADD_TIME_ENTRY_FROM_TILE) {
            quickAddRequestId += 1
            openAddFromQuickSettings = true
            intent.action = null
        }
    }

    companion object {
        const val ACTION_ADD_TIME_ENTRY_FROM_TILE = "app.tinks.tink.action.ADD_TIME_ENTRY_FROM_TILE"
    }
}

@Preview(showBackground = true)
@Composable
fun MyAppPreview() {
    TinkTheme() {
        MyApp()
    }
}
