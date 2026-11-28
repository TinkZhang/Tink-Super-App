package app.tinks.tink.zi.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import app.tinks.tink.R

/** Lightweight 8-bit chiptune SoundPool effects for the falling-character game. */
class ZiGameAudio(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loaded = mutableSetOf<Int>()
    private val fireworkId: Int
    private val missId: Int
    private val clearId: Int
    private var closed = false

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
        fireworkId = pool.load(context, R.raw.zi_game_firework, 1)
        missId = pool.load(context, R.raw.zi_game_miss, 1)
        clearId = pool.load(context, R.raw.zi_game_clear, 1)
    }

    fun playFirework() = play(fireworkId, 0.7f)

    fun playMiss() = play(missId, 0.55f)

    fun playStageClear() = play(clearId, 0.75f)

    private fun play(soundId: Int, volume: Float) {
        if (!closed && soundId in loaded) {
            pool.play(soundId, volume, volume, 1, 0, 1f)
        }
    }

    fun close() {
        if (!closed) {
            closed = true
            pool.release()
        }
    }
}
