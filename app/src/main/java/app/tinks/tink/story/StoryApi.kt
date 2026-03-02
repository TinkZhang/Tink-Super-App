package app.tinks.tink.story

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StoryApi {
    @GET("story/story_list")
    suspend fun getStoryList(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): List<StoryDto>

    @GET("story/{storyId}")
    suspend fun getStory(@Path("storyId") storyId: String): StoryDto

    @DELETE("story/{storyId}")
    suspend fun deleteStory(@Path("storyId") storyId: String)
}

@Serializable
data class StoryDto(
    val title: String,
    val content: String? = null,
    val id: String,
    @SerialName("created_at")
    val createdAt: String,
    val length: Int,
    @SerialName("unique_char")
    val uniqueChar: Int,
)
