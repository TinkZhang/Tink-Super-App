package app.tinks.tink.weight

import app.tinks.tink.weight.data.Weight
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.time.Instant

interface WeightApi {
    @GET("weight")
    suspend fun getWeights(): List<WeightDto>

    @POST("weight")
    suspend fun createWeight(
        @Body payload: WeightCreateRequest,
    ): WeightDto

    @DELETE("weight/{weightId}")
    suspend fun deleteWeight(
        @Path("weightId") weightId: Int,
    )
}

@Serializable
data class WeightDto(
    val id: Int,
    @SerialName("created_at")
    val createdAt: String,
    val weight: Double,
)

@Serializable
data class WeightCreateRequest(
    val weight: Double,
)

fun WeightDto.toDomain(): Weight = Weight(
    id = id,
    weight = weight,
    createdTime = runCatching { Instant.parse(createdAt).toEpochMilli() }
        .getOrElse { Instant.EPOCH.toEpochMilli() },
)
