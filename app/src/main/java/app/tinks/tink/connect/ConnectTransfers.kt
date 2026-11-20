package app.tinks.tink.connect

import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.Base64
import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject

internal const val CONNECT_CHUNK_SIZE = 6 * 1024 * 1024
private const val TUS_VERSION = "1.0.0"

internal class ConnectTransferException(val status: Int) : IOException("Storage returned HTTP $status")

internal data class PendingConnectUpload(
    val source: File,
    val request: ConnectUploadRequest,
    var ticket: ConnectUpload? = null,
    var location: HttpUrl? = null,
    var started: Boolean = false,
)

class ConnectTransfers internal constructor(private val client: OkHttpClient) {
    // Transfer capabilities must never pass through the app's body logger.
    @Inject constructor() : this(
        OkHttpClient.Builder()
            .followRedirects(false)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    )

    internal fun upload(pending: PendingConnectUpload, ticket: ConnectUpload, onProgress: (Float) -> Unit) {
        val request = pending.request
        if (request.size == 0L) {
            execute(Request.Builder().url(ticket.signedUploadUrl)
                .header("apikey", ticket.publishableKey)
                .put(ByteArray(0).toRequestBody(request.contentType.toMediaType()))
                .build()) {}
            return
        }
        val location = pending.location ?: create(ticket, request).also { pending.location = it }
        var offset = execute(tusRequest(location, ticket).head().build()) { it.offset() }
        check(offset in 0..request.size) { "Storage reported an invalid upload offset" }
        RandomAccessFile(pending.source, "r").use { source ->
            source.seek(offset)
            while (offset < request.size) {
                val chunk = ByteArray(minOf(CONNECT_CHUNK_SIZE.toLong(), request.size - offset).toInt())
                source.readFully(chunk)
                val next = execute(
                    tusRequest(location, ticket)
                        .header("Upload-Offset", offset.toString())
                        .patch(chunk.toRequestBody("application/offset+octet-stream".toMediaType()))
                        .build()
                ) { it.offset() }
                check(next == offset + chunk.size) { "Storage reported an invalid upload offset" }
                offset = next
                onProgress((offset.toDouble() / request.size).toFloat())
            }
        }
    }

    private fun create(ticket: ConnectUpload, request: ConnectUploadRequest): HttpUrl {
        val endpoint = ticket.uploadUrl.toHttpUrl()
        return execute(
            tusRequest(endpoint, ticket)
                .header("Upload-Length", request.size.toString())
                .header("Upload-Metadata", connectUploadMetadata(ticket, request.contentType))
                .post(ByteArray(0).toRequestBody()).build()
        ) { response ->
            val location = response.header("Location")?.let(endpoint::resolve)
                ?: throw IOException("Missing upload location")
            if (location.scheme != endpoint.scheme || location.host != endpoint.host || location.port != endpoint.port) {
                throw IOException("Unexpected upload location")
            }
            location
        }
    }

    internal fun download(url: String, target: OutputStream) {
        execute(Request.Builder().url(url).build()) { response ->
            val body = response.body ?: throw IOException("Empty download response")
            body.byteStream().use { it.copyTo(target) }
        }
    }

    private fun <T> execute(request: Request, read: (Response) -> T): T =
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw ConnectTransferException(response.code)
            read(response)
        }

    private fun Response.offset(): Long = header("Upload-Offset")?.toLongOrNull()
        ?: throw IOException("Missing upload offset")

    private fun tusRequest(url: HttpUrl, ticket: ConnectUpload) = Request.Builder().url(url)
        .header("Tus-Resumable", TUS_VERSION)
        .header("x-signature", ticket.token)
        .header("apikey", ticket.publishableKey)
}

internal fun connectUploadMetadata(ticket: ConnectUpload, mime: String): String =
    mapOf("bucketName" to ticket.bucket, "objectName" to ticket.objectPath, "contentType" to mime)
        .entries.joinToString(",") { (key, value) ->
            "$key ${Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))}"
        }
