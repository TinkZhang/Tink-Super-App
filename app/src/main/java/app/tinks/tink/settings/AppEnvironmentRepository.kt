package app.tinks.tink.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ApiEnvironment {
    Dev,
    Prod,
}

@Singleton
class AppEnvironmentRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val environment = MutableStateFlow(loadEnvironment())

    fun observeEnvironment(): Flow<ApiEnvironment> = environment.asStateFlow()

    fun currentEnvironment(): ApiEnvironment = environment.value

    suspend fun setEnvironment(value: ApiEnvironment) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_API_ENVIRONMENT, value.name).apply()
        environment.value = value
    }

    private fun loadEnvironment(): ApiEnvironment {
        val stored = prefs.getString(KEY_API_ENVIRONMENT, null)
        return runCatching { stored?.let(ApiEnvironment::valueOf) }.getOrNull()
            ?: DEFAULT_ENVIRONMENT
    }

    private companion object {
        const val PREFS_NAME = "app_environment"
        const val KEY_API_ENVIRONMENT = "api_environment"
        val DEFAULT_ENVIRONMENT = ApiEnvironment.Dev
    }
}
