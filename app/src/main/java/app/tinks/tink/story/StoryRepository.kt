package app.tinks.tink.story

import app.tinks.tink.network.ApiResult
import app.tinks.tink.network.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepository @Inject constructor(
    private val api: StoryApi,
) {
    fun getStoryList(page: Int, size: Int): Flow<ApiResult<List<Story>>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall {
            api.getStoryList(page = page, size = size).map { it.toDomain() }
        })
    }.flowOn(Dispatchers.IO)

    fun getStory(storyId: String): Flow<ApiResult<Story>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall {
            api.getStory(storyId = storyId).toDomain()
        })
    }.flowOn(Dispatchers.IO)

    fun deleteStory(storyId: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        emit(safeApiCall {
            api.deleteStory(storyId = storyId)
        })
    }.flowOn(Dispatchers.IO)
}
