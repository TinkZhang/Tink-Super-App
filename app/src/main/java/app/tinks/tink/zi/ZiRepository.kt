package app.tinks.tink.zi

import android.content.Context
import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZiRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ZiApi,
) {
    private val allZis = MutableStateFlow<List<Zi>>(emptyList())
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAllLearntZisFlow(): Flow<List<Zi>> = allZis.asStateFlow().map { zis ->
        zis.filter { it.proficiency >= 5 }
    }

    fun putZi(zi: String, proficiency: Int) = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall {
            try {
                api.putZi(
                    request = PutZiRequest(zi, proficiency)
                )
            } catch (e: Exception) {
                println("API call failed with exception: ${e.message}")
                println("Exception stack trace: $e")
                throw e
            }
        })
    }.flowOn(Dispatchers.IO)

    fun getAllZis(): Flow<ApiResult<List<Zi>>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall {
            try {
                api.getZis().map { it.toDomain() }
            } catch (e: Exception) {
                println("API call failed with exception: ${e.message}")
                println("Exception stack trace: $e")
                throw e
            }
        })
    }.flowOn(Dispatchers.IO)

    fun generateStory(length: Int): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall {
            try {
                api.generateStory(
                    request = StoryGenerateRequest(length = length)
                )
            } catch (e: Exception) {
                println("API call failed with exception: ${e.message}")
                println("Exception stack trace: $e")
                throw e
            }
        })
    }.flowOn(Dispatchers.IO)

    suspend fun getStoryLength(): Int = withContext(Dispatchers.IO) {
        prefs.getInt(KEY_STORY_LENGTH, DEFAULT_STORY_LENGTH)
    }

    suspend fun setStoryLength(length: Int) = withContext(Dispatchers.IO) {
        prefs.edit().putInt(KEY_STORY_LENGTH, length).apply()
    }

    private companion object {
        const val PREFS_NAME = "zi_prefs"
        const val KEY_STORY_LENGTH = "story_length"
        const val DEFAULT_STORY_LENGTH = 200
    }
}
