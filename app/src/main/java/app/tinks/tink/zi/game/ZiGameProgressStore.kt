package app.tinks.tink.zi.game

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ZiGameSavedProgress(
    val highestUnlockedStage: Int = 1,
    val bestScore: Int = 0,
    val stageStars: Map<Int, Int> = emptyMap(),
    val speed: ZiGameSpeed = ZiGameSpeed.Slow,
)

interface ZiGameProgressRepository {
    fun load(): ZiGameSavedProgress
    fun saveSpeed(speed: ZiGameSpeed)
    fun recordStageResult(stageNumber: Int, score: Int, stars: Int): ZiGameSavedProgress
}

@Singleton
class ZiGameProgressStore @Inject constructor(
    @ApplicationContext context: Context,
) : ZiGameProgressRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): ZiGameSavedProgress {
        val unlocked = prefs.getInt(KEY_UNLOCKED_STAGE, 1).coerceAtLeast(1)
        val bestScore = prefs.getInt(KEY_BEST_SCORE, 0).coerceAtLeast(0)
        val speedName = prefs.getString(KEY_SPEED, ZiGameSpeed.Slow.name)
        val speed = ZiGameSpeed.entries.firstOrNull { it.name == speedName } ?: ZiGameSpeed.Slow
        val stars = decodeStars(prefs.getString(KEY_STAGE_STARS, "").orEmpty())
        return ZiGameSavedProgress(
            highestUnlockedStage = unlocked,
            bestScore = bestScore,
            stageStars = stars,
            speed = speed,
        )
    }

    override fun saveSpeed(speed: ZiGameSpeed) {
        prefs.edit().putString(KEY_SPEED, speed.name).apply()
    }

    override fun recordStageResult(stageNumber: Int, score: Int, stars: Int): ZiGameSavedProgress {
        val current = load()
        val nextUnlocked = maxOf(current.highestUnlockedStage, stageNumber + 1)
        val nextBest = maxOf(current.bestScore, score)
        val updatedStars = current.stageStars.toMutableMap().apply {
            val prev = this[stageNumber] ?: 0
            this[stageNumber] = maxOf(prev, stars)
        }
        prefs.edit()
            .putInt(KEY_UNLOCKED_STAGE, nextUnlocked)
            .putInt(KEY_BEST_SCORE, nextBest)
            .putString(KEY_STAGE_STARS, encodeStars(updatedStars))
            .apply()
        return current.copy(
            highestUnlockedStage = nextUnlocked,
            bestScore = nextBest,
            stageStars = updatedStars,
        )
    }

    private fun encodeStars(stars: Map<Int, Int>): String =
        stars.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }

    private fun decodeStars(raw: String): Map<Int, Int> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(",")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size != 2) return@mapNotNull null
                val stage = parts[0].toIntOrNull() ?: return@mapNotNull null
                val star = parts[1].toIntOrNull() ?: return@mapNotNull null
                stage to star.coerceIn(1, 3)
            }
            .toMap()
    }

    private companion object {
        const val PREFS_NAME = "zi_game_prefs"
        const val KEY_UNLOCKED_STAGE = "highest_unlocked_stage"
        const val KEY_BEST_SCORE = "best_score"
        const val KEY_STAGE_STARS = "stage_stars"
        const val KEY_SPEED = "game_speed"
    }
}
