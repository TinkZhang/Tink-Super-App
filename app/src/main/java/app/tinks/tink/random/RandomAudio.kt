package app.tinks.tink.random

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.tinks.tink.R

/** Bundled procedural effects and device-local Chinese speech; never uses a server. */
class RandomAudio(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
    ).build()
    private val loaded = mutableSetOf<Int>()
    private val sounds: Map<RandomType, Int>
    private var stream = 0
    private var closed = false
    private var ready = false
    var speechUnavailable by mutableStateOf(false)
        private set
    private var tts: TextToSpeech? = null

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) }
        sounds = mapOf(
            RandomType.Dice to pool.load(context, R.raw.random_dice, 1),
            RandomType.Wheel to pool.load(context, R.raw.random_wheel, 1),
            RandomType.Coin to pool.load(context, R.raw.random_coin, 1),
        )
        tts = TextToSpeech(context.applicationContext) { status ->
            if (!closed) {
                val voice = if (status == TextToSpeech.SUCCESS) tts?.voices?.firstOrNull {
                    it.locale.language == "zh" && !it.isNetworkConnectionRequired && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features
                } else null
                ready = voice != null && tts?.setVoice(voice) == TextToSpeech.SUCCESS
                speechUnavailable = !ready
            }
        }
    }

    fun start(type: RandomType, enabled: Boolean) {
        stop()
        val id = sounds.getValue(type)
        if (!closed && enabled && id in loaded) stream = pool.play(id, .65f, .65f, 1, -1, 1f)
    }

    fun speak(text: String, enabled: Boolean) {
        if (!closed && enabled && ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "random-result")
    }

    fun stop() {
        if (closed) return
        if (stream != 0) pool.stop(stream)
        stream = 0
        tts?.stop()
    }

    fun close() { if (!closed) { stop(); closed = true; pool.release(); tts?.shutdown() } }
}
