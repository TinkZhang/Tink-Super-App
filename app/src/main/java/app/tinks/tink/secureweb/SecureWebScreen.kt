package app.tinks.tink.secureweb

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

const val SECURE_WEB_URL = "https://t66y.com/thread0806.php?fid=7&isMobile=yes"

interface SecureWebAuthenticator {
    fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )
}

@Composable
fun SecureWebScreen(
    modifier: Modifier = Modifier,
    url: String = SECURE_WEB_URL,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    authenticator: SecureWebAuthenticator = rememberPlatformSecureWebAuthenticator(),
    webContent: @Composable (String) -> Unit = { SecureWebView(it) },
) {
    var isUnlocked by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var authRequestId by remember { mutableIntStateOf(0) }

    fun requestAuthentication() {
        isUnlocked = false
        isAuthenticating = true
        errorMessage = null
        authRequestId += 1
    }

    LaunchedEffect(Unit) {
        requestAuthentication()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                requestAuthentication()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(authRequestId) {
        if (authRequestId == 0) return@LaunchedEffect
        authenticator.authenticate(
            onSuccess = {
                isUnlocked = true
                isAuthenticating = false
                errorMessage = null
            },
            onError = { message ->
                isUnlocked = false
                isAuthenticating = false
                errorMessage = message
            },
        )
    }

    if (isUnlocked) {
        webContent(url)
    } else {
        SecureWebLockedContent(
            isAuthenticating = isAuthenticating,
            errorMessage = errorMessage,
            onRetry = { requestAuthentication() },
            modifier = modifier,
        )
    }
}

@Composable
fun SecureWebLockedContent(
    isAuthenticating: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("secure_web_locked_content"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Fingerprint,
            contentDescription = null,
            modifier = Modifier.testTag("secure_web_fingerprint_icon"),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Fingerprint required",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = errorMessage ?: "Unlock to open Baidu in this app.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (isAuthenticating) {
            CircularProgressIndicator(
                modifier = Modifier.testTag("secure_web_auth_progress"),
            )
        } else {
            Button(
                onClick = onRetry,
                modifier = Modifier.testTag("secure_web_unlock_button"),
            ) {
                Text("Unlock")
            }
        }
    }
}

@Composable
private fun rememberPlatformSecureWebAuthenticator(): SecureWebAuthenticator {
    val context = LocalContext.current
    return remember(context) {
        PlatformSecureWebAuthenticator(context)
    }
}

private class PlatformSecureWebAuthenticator(
    private val context: Context,
) : SecureWebAuthenticator {

    override fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val activity = context.findActivity()
        if (activity == null) {
            onError("Unable to open fingerprint prompt.")
            return
        }

        val biometricManager = activity.getSystemService(BiometricManager::class.java)
        if (biometricManager == null) {
            onError("Fingerprint unlock is unavailable on this device.")
            return
        }

        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        val canAuthenticate = biometricManager.canAuthenticate(authenticators)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("Fingerprint unlock is unavailable on this device.")
            return
        }

        val prompt = BiometricPrompt.Builder(activity)
            .setTitle("Unlock Baidu")
            .setSubtitle("Use fingerprint to open the protected page")
            .setAllowedAuthenticators(authenticators)
            .setNegativeButton("Cancel", activity.mainExecutor) { _, _ ->
                onError("Fingerprint unlock was canceled.")
            }
            .build()

        prompt.authenticate(
            CancellationSignal(),
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            },
        )
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun SecureWebView(url: String) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
        canGoBack = webView?.canGoBack() == true
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        canGoBack = view.canGoBack()
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView,
                        url: String?,
                        isReload: Boolean,
                    ) {
                        canGoBack = view.canGoBack()
                    }
                }
                webView = this
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
            canGoBack = webView.canGoBack()
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("secure_web_webview"),
    )
}
