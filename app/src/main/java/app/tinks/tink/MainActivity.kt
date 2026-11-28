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
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import app.tinks.tink.ui.theme.TinkTheme
import app.tinks.tink.widget.ReadKeeperWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var sharedYtUrl by mutableStateOf<String?>(null)
    private var ytShareRequestId by mutableStateOf<String?>(null)
    private var quickAddRequestId by mutableIntStateOf(0)
    private var openAddFromQuickSettings by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleLaunchIntent(intent) else {
            sharedYtUrl = savedInstanceState.getString("yt-share-url")
            ytShareRequestId = savedInstanceState.getString("yt-share-id")
        }
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
                        sharedYtUrl = sharedYtUrl,
                        ytShareRequestId = ytShareRequestId,
                        onYtShareConsumed = { sharedYtUrl = null; ytShareRequestId = null },
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("yt-share-url", sharedYtUrl)
        outState.putString("yt-share-id", ytShareRequestId)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            ReadKeeperWidget().updateAll(this@MainActivity)
        }
    }

    private fun handleLaunchIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val url = app.tinks.tink.yt.youtubeUrlFromShare(intent)
                if (url != null) {
                    sharedYtUrl = url
                    ytShareRequestId = java.util.UUID.randomUUID().toString()
                } else android.widget.Toast.makeText(this, "Share a YouTube video or playlist link to Tink.", android.widget.Toast.LENGTH_LONG).show()
                intent.action = null
                intent.removeExtra(Intent.EXTRA_TEXT)
            }
            ACTION_ADD_TIME_ENTRY_FROM_TILE -> {
                quickAddRequestId += 1
                openAddFromQuickSettings = true
                intent.action = null
            }
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
