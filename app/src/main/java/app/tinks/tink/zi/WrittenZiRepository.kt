package app.tinks.tink.zi

import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class WrittenZiListState(
    val characters: List<String>? = null,
    val isLoading: Boolean = true,
    val failed: Boolean = false,
)

internal data class WrittenZiBatchResult(val confirmedCount: Int, val addedCount: Int)

@Singleton
class WrittenZiRepository @Inject constructor(private val api: ZiApi) {
    private val lock = Mutex()
    private val list = MutableStateFlow(WrittenZiListState())
    val state = list.asStateFlow()

    suspend fun refresh() = lock.withLock {
        val previous = list.value
        list.value = previous.copy(isLoading = true, failed = false)
        try {
            list.value = when (val result = safeApiCall { api.getWrittenZis() }) {
                is ApiResult.Success -> WrittenZiListState(result.data, isLoading = false)
                is ApiResult.Error -> {
                    (result.throwable as? CancellationException)?.let { throw it }
                    previous.copy(isLoading = false, failed = true)
                }
                ApiResult.Loading -> previous
            }
        } catch (cancelled: CancellationException) {
            list.value = previous
            throw cancelled
        }
    }

    internal suspend fun saveBatch(characters: List<String>): WrittenZiBatchResult {
        var added = 0
        for ((index, character) in characters.withIndex()) {
            val result = save(character)
            if (result !is ApiResult.Success) return WrittenZiBatchResult(index, added)
            if (result.data.added) added++
        }
        return WrittenZiBatchResult(characters.size, added)
    }

    suspend fun save(character: String): ApiResult<WrittenZiResult> = lock.withLock {
        val result = safeApiCall { api.putWrittenZi(WrittenZiRequest(character)) }
        if (result is ApiResult.Error) {
            (result.throwable as? CancellationException)?.let { throw it }
        }
        if (result is ApiResult.Success) {
            val current = list.value
            val characters = current.characters
            if (characters != null && character !in characters) {
                list.value = current.copy(characters = (characters + character).sortedWith(
                    compareBy { it.codePointAt(0) }
                ))
            }
        }
        result
    }
}
