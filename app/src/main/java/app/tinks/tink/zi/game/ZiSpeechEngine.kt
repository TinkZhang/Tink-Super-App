package app.tinks.tink.zi.game

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

interface ZiSpeechEngine {
    fun start(
        modelFiles: ZiVoiceModelFiles,
        onPartialText: (String) -> Unit,
        onMicLevel: (Float) -> Unit,
        onError: (String) -> Unit,
    )

    /** Clears streaming ASR buffer when switching to the next falling character. */
    fun resetUtterance()

    fun stop()
}

@Singleton
class SherpaOnnxSpeechEngine @Inject constructor() : ZiSpeechEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val resetRequested = AtomicBoolean(false)
    private val running = AtomicBoolean(false)

    private var sessionJob: Job? = null
    private var cachedFiles: ZiVoiceModelFiles? = null
    private var cachedRecognizer: OnlineRecognizer? = null

    override fun start(
        modelFiles: ZiVoiceModelFiles,
        onPartialText: (String) -> Unit,
        onMicLevel: (Float) -> Unit,
        onError: (String) -> Unit,
    ) {
        stop()
        running.set(true)
        resetRequested.set(false)

        sessionJob = scope.launch {
            var audioRecord: AudioRecord? = null
            var stream: OnlineStream? = null
            try {
                val recognizer = getOrCreateRecognizer(modelFiles)
                stream = recognizer.createStream()
                audioRecord = createAudioRecord()
                if (audioRecord == null || audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    onError("麦克风初始化失败，请确认麦克风未被其他应用占用")
                    return@launch
                }

                audioRecord.startRecording()
                val chunkSamples = SAMPLE_RATE / 10 // 100ms chunks
                val pcmBuffer = ShortArray(chunkSamples)
                val floatBuffer = FloatArray(chunkSamples)
                var lastEmittedText = ""

                while (isActive && running.get()) {
                    if (resetRequested.compareAndSet(true, false)) {
                        recognizer.reset(stream)
                        lastEmittedText = ""
                    }

                    val read = audioRecord.read(pcmBuffer, 0, pcmBuffer.size)
                    if (read <= 0) continue

                    var sumSq = 0.0
                    for (i in 0 until read) {
                        val sample = pcmBuffer[i] / 32768.0f
                        floatBuffer[i] = sample
                        sumSq += sample * sample
                    }
                    val rms = sqrt(sumSq / read).toFloat()
                    onMicLevel((rms * 7.5f).coerceIn(0f, 1f))

                    val slice = if (read == floatBuffer.size) floatBuffer else floatBuffer.copyOf(read)
                    stream.acceptWaveform(slice, SAMPLE_RATE)
                    while (recognizer.isReady(stream)) {
                        recognizer.decode(stream)
                    }

                    val currentText = recognizer.getResult(stream).text.trim()
                    if (currentText.isNotEmpty() && currentText != lastEmittedText) {
                        lastEmittedText = currentText
                        onPartialText(currentText)
                    }

                    if (recognizer.isEndpoint(stream)) {
                        recognizer.reset(stream)
                        lastEmittedText = ""
                    }
                }
            } catch (t: Throwable) {
                if (running.get()) {
                    onError("语音识别异常：${t.message ?: "请重新开始"}")
                }
            } finally {
                runCatching { audioRecord?.stop() }
                runCatching { audioRecord?.release() }
                runCatching { stream?.release() }
                onMicLevel(0f)
            }
        }
    }

    override fun resetUtterance() {
        resetRequested.set(true)
    }

    override fun stop() {
        running.set(false)
        sessionJob?.cancel()
        sessionJob = null
    }

    @Synchronized
    private fun getOrCreateRecognizer(files: ZiVoiceModelFiles): OnlineRecognizer {
        val existing = cachedRecognizer
        if (existing != null && cachedFiles == files) {
            return existing
        }
        runCatching { existing?.release() }
        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = FEATURE_DIM),
            modelConfig = OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = files.encoderPath,
                    decoder = files.decoderPath,
                    joiner = files.joinerPath,
                ),
                tokens = files.tokensPath,
                numThreads = 2,
                debug = false,
                provider = "cpu",
                modelType = "zipformer",
            ),
            enableEndpoint = true,
            decodingMethod = "greedy_search",
        )
        return OnlineRecognizer(assetManager = null, config = config).also {
            cachedRecognizer = it
            cachedFiles = files
        }
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(): AudioRecord? {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(SAMPLE_RATE / 2)

        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
        )
        for (source in sources) {
            val record = runCatching {
                AudioRecord(
                    source,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2,
                )
            }.getOrNull()
            if (record != null && record.state == AudioRecord.STATE_INITIALIZED) {
                return record
            }
            runCatching { record?.release() }
        }
        return null
    }

    private companion object {
        const val SAMPLE_RATE = 16000
        const val FEATURE_DIM = 80
    }
}
