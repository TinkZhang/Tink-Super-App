package app.tinks.tink.merriam.network

import app.tinks.tink.merriam.data.StatDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MerriamApi {
    @GET("merriam/stat")
    suspend fun getStat(): StatDto

    @POST("merriam")
    suspend fun postMerriam(
        @Body request: RootPostDto
    )
}

@Serializable
data class RootPostDto(
    @SerialName("root_id")
    val rootId: Int,
    val root: String,
)
