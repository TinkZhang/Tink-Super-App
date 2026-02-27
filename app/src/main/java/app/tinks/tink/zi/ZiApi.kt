package app.tinks.tink.zi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface ZiApi {
    @GET("zi/")
    suspend fun getZis(
        @Query("size") size: Int? = null,
        @Query("proficiency") proficiency: Int? = null,
    ): List<ZiDto>

    @PUT("zi/")
    suspend fun putZi(@Body request: PutZiRequest)

    @GET("zi/learnt_zi_num")
    suspend fun getLearntZiNum(): Int
}

@Serializable
data class PutZiRequest(
    val zis: String,
    val proficiency: Int,
)

@Serializable
data class ZiDto(
    val zi: String,
    val proficiency: Int,
    @SerialName("last_date")
    val lastDate: String,
)
