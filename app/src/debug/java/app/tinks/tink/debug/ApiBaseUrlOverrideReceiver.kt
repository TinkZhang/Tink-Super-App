package app.tinks.tink.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ApiBaseUrlOverrideReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        when (intent.action) {
            ACTION_SET -> {
                val baseUrl = intent.getStringExtra(EXTRA_BASE_URL)?.trim().orEmpty()
                if (baseUrl.isNotBlank()) {
                    prefs.edit().putString(KEY_BASE_URL, baseUrl.ensureTrailingSlash()).apply()
                }
            }
            ACTION_CLEAR -> prefs.edit().remove(KEY_BASE_URL).apply()
        }
    }

    private fun String.ensureTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"

    private companion object {
        const val ACTION_SET = "app.tinks.tink.debug.SET_API_BASE_URL"
        const val ACTION_CLEAR = "app.tinks.tink.debug.CLEAR_API_BASE_URL"
        const val EXTRA_BASE_URL = "base_url"
        const val PREFS_NAME = "debug_api_base_url_override"
        const val KEY_BASE_URL = "base_url"
    }
}
