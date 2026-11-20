package app.tinks.tink.connect

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ConnectTransfersTest {
    @Test
    fun downloadChecksReceivedBytesAgainstHubMetadata() {
        val contents = byteArrayOf(1, 2, 3)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(contents.toResponseBody()).build()
        }.build()
        val staged = File.createTempFile("connect-test", ".download")
        try {
            val transfer = ConnectTransfers(client)
            transfer.download("https://storage.example/file", staged, 3)
            assertArrayEquals(contents, staged.readBytes())
            assertThrows(IOException::class.java) {
                transfer.download("https://storage.example/file", staged, 4)
            }
        } finally {
            staged.delete()
        }
    }

    @Test
    fun resumesAtServerOffsetAfterAnInterruptedChunk() {
        val payload = ByteArray(CONNECT_CHUNK_SIZE + 41) { (it % 251).toByte() }
        val received = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()
        var failFinalChunk = true
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals("token", request.header("x-signature"))
            val response = Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .message("Test response").body(ByteArray(0).toResponseBody())
            when (request.method) {
                "POST" -> response.code(201).header("Location", "/tus/file")
                "HEAD" -> response.code(200).header("Upload-Offset", received.size().toString())
                "PATCH" -> {
                    val offset = checkNotNull(request.header("Upload-Offset")).toInt()
                    offsets.add(offset)
                    val chunk = Buffer()
                    checkNotNull(request.body).writeTo(chunk)
                    if (offset > 0 && failFinalChunk) {
                        failFinalChunk = false
                        response.code(503)
                    } else {
                        received.write(chunk.readByteArray())
                        response.code(204).header("Upload-Offset", received.size().toString())
                    }
                }
                else -> error("Unexpected test request")
            }
            response.build()
        }.build()
        val source = File.createTempFile("connect-test", ".data")
        try {
            source.writeBytes(payload)
            val root = "https://storage.example"
            val ticket = ConnectUpload("id", "$root/tus", "token", "key", "$root/signed", "bucket", "object")
            val pending = PendingConnectUpload(source, ConnectUploadRequest("unknown.bin", "application/octet-stream", source.length()))
            val transfer = ConnectTransfers(client)
            assertThrows(ConnectTransferException::class.java) { transfer.upload(pending, ticket) {} }
            transfer.upload(pending, ticket) {}
            assertEquals(listOf(0, CONNECT_CHUNK_SIZE, CONNECT_CHUNK_SIZE), offsets)
            assertArrayEquals(payload, received.toByteArray())
        } finally {
            source.delete()
        }
    }
}
