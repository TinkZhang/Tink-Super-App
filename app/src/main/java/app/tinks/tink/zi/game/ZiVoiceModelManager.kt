package app.tinks.tink.zi.game

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ZiVoiceModelFiles(
    val encoderPath: String,
    val decoderPath: String,
    val joinerPath: String,
    val tokensPath: String,
)

interface ZiVoiceModelProvider {
    fun isModelReady(): Boolean
    fun modelFilesOrNull(): ZiVoiceModelFiles?
    fun ensureModelReady(): Flow<ZiVoiceModelState>
}

@Singleton
class ZiVoiceModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : ZiVoiceModelProvider {

    private data class ModelArtifact(
        val fileName: String,
        val expectedMinBytes: Long,
        val approxBytes: Long,
    )

    private val artifacts = listOf(
        ModelArtifact("encoder-epoch-99-avg-1.int8.onnx", expectedMinBytes = 10_000_000L, approxBytes = 22_600_000L),
        ModelArtifact("decoder-epoch-99-avg-1.int8.onnx", expectedMinBytes = 500_000L, approxBytes = 1_980_000L),
        ModelArtifact("joiner-epoch-99-avg-1.int8.onnx", expectedMinBytes = 500_000L, approxBytes = 1_880_000L),
        ModelArtifact("tokens.txt", expectedMinBytes = 10_000L, approxBytes = 50_000L),
    )

    // Try mainland-friendly mirror first, then official Hugging Face origin.
    private val mirrorBases = listOf(
        "https://hf-mirror.com/csukuangfj/sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23/resolve/main",
        "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23/resolve/main",
    )

    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun modelDir(): File = File(context.filesDir, "zi-voice-model/zipformer-zh-14m-int8")

    override fun isModelReady(): Boolean = modelFilesOrNull() != null

    override fun modelFilesOrNull(): ZiVoiceModelFiles? {
        val dir = modelDir()
        val files = artifacts.map { artifact ->
            val file = File(dir, artifact.fileName)
            if (!file.isFile || file.length() < artifact.expectedMinBytes) return null
            file
        }
        return ZiVoiceModelFiles(
            encoderPath = files[0].absolutePath,
            decoderPath = files[1].absolutePath,
            joinerPath = files[2].absolutePath,
            tokensPath = files[3].absolutePath,
        )
    }

    override fun ensureModelReady(): Flow<ZiVoiceModelState> = flow {
        if (isModelReady()) {
            emit(ZiVoiceModelState.Ready)
            return@flow
        }

        val dir = modelDir()
        if (!dir.exists() && !dir.mkdirs()) {
            emit(ZiVoiceModelState.Error("无法创建离线语音模型目录"))
            return@flow
        }

        val totalApproxBytes = artifacts.sumOf { it.approxBytes }.toFloat()
        val totalMb = totalApproxBytes / BYTES_PER_MB
        var completedBytes = 0L

        for (artifact in artifacts) {
            val target = File(dir, artifact.fileName)
            if (target.isFile && target.length() >= artifact.expectedMinBytes) {
                completedBytes += target.length()
                continue
            }

            try {
                downloadArtifactWithFallback(
                    artifact = artifact,
                    targetFile = target,
                    onBytesDelta = { bytesSoFarForFile ->
                        val totalDownloaded = (completedBytes + bytesSoFarForFile).toFloat()
                        val progress = (totalDownloaded / totalApproxBytes).coerceIn(0f, 0.99f)
                        emit(
                            ZiVoiceModelState.Downloading(
                                progress = progress,
                                downloadedMb = totalDownloaded / BYTES_PER_MB,
                                totalMb = totalMb,
                            ),
                        )
                    },
                )
                completedBytes += target.length()
            } catch (e: Exception) {
                emit(ZiVoiceModelState.Error("离线语音包下载失败：${e.message ?: "请检查网络后重试"}"))
                return@flow
            }
        }

        if (isModelReady()) {
            emit(ZiVoiceModelState.Ready)
        } else {
            emit(ZiVoiceModelState.Error("语音模型校验未通过，请重新下载"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadArtifactWithFallback(
        artifact: ModelArtifact,
        targetFile: File,
        onBytesDelta: suspend (Long) -> Unit,
    ) {
        var lastError: Exception? = null
        for (baseUrl in mirrorBases) {
            val partFile = File(targetFile.parentFile, "${artifact.fileName}.part")
            try {
                val request = Request.Builder()
                    .url("$baseUrl/${artifact.fileName}")
                    .build()
                downloadClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IOException("Empty response body")
                    body.byteStream().use { input ->
                        partFile.outputStream().use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var fileBytes = 0L
                            var lastReportedBytes = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                fileBytes += read
                                if (fileBytes - lastReportedBytes >= 128 * 1024) {
                                    lastReportedBytes = fileBytes
                                    onBytesDelta(fileBytes)
                                }
                            }
                            output.flush()
                            onBytesDelta(fileBytes)
                        }
                    }
                }
                if (partFile.length() < artifact.expectedMinBytes) {
                    throw IOException("Incomplete download (${partFile.length()} bytes)")
                }
                if (targetFile.exists()) targetFile.delete()
                if (!partFile.renameTo(targetFile)) {
                    throw IOException("Failed to finalize ${artifact.fileName}")
                }
                return
            } catch (e: Exception) {
                partFile.delete()
                lastError = e
            }
        }
        throw lastError ?: IOException("Unable to download ${artifact.fileName}")
    }

    private companion object {
        const val BYTES_PER_MB = 1024f * 1024f
    }
}
