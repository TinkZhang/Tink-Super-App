package app.tinks.tink.connect

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

@Serializable
data class ConnectFile(
    val id: String,
    val name: String,
    @SerialName("content_type") val contentType: String,
    val size: Long,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ConnectHub(val files: List<ConnectFile> = emptyList(), val text: String = "")

@Serializable
data class ConnectText(val text: String)

@Serializable
data class ConnectUploadRequest(
    val name: String,
    @SerialName("content_type") val contentType: String,
    val size: Long,
)

@Serializable
data class ConnectUpload(
    val id: String,
    @SerialName("upload_url") val uploadUrl: String,
    val token: String,
    @SerialName("publishable_key") val publishableKey: String,
    @SerialName("signed_upload_url") val signedUploadUrl: String,
    val bucket: String,
    @SerialName("object_path") val objectPath: String,
)

@Serializable
data class ConnectDownload(val url: String)

interface ConnectApi {
    @GET("connect") suspend fun hub(): ConnectHub
    @PUT("connect/text") suspend fun share(@Body text: ConnectText): ConnectText
    @POST("connect/files") suspend fun begin(@Body file: ConnectUploadRequest): ConnectUpload
    @POST("connect/files/{id}/complete") suspend fun complete(@Path("id") id: String): ConnectFile
    @GET("connect/files/{id}/download") suspend fun download(@Path("id") id: String): ConnectDownload
    @DELETE("connect/files/{id}") suspend fun delete(@Path("id") id: String)
    @DELETE("connect") suspend fun clear()
}
