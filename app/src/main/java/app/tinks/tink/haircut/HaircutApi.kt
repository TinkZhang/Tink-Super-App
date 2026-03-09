package app.tinks.tink.haircut

import app.tinks.tink.haircut.data.Haircut
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface HaircutApi {
    @GET("haircut")
    suspend fun getHaircuts(): List<HaircutDto>

    @POST("haircut")
    suspend fun createHaircut(
        @Body payload: HaircutCreateRequest,
    ): HaircutDto

    @DELETE("haircut/{haircutId}")
    suspend fun deleteHaircut(
        @Path("haircutId") haircutId: Int,
    )
}

@Serializable
data class HaircutDto(
    val id: Int,
    @SerialName("created_at")
    val createdAt: LocalDate,
    @SerialName("shop_name")
    val shopName: String,
    val price: Int,
)

@Serializable
data class HaircutCreateRequest(
    @SerialName("shop_name")
    val shopName: String,
    val price: Int,
    @SerialName("created_at")
    val createdAt: LocalDate? = null,
)

fun HaircutDto.toDomain(): Haircut = Haircut(
    id = id,
    price = price,
    date = createdAt,
    shopName = shopName,
)
