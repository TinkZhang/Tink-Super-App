package app.tinks.tink.zi.game

import androidx.compose.ui.graphics.Color

enum class ZiGameSpeed(
    val label: String,
    /** Normalized vertical travel per second (0f = top, 1f = ground defense line). */
    val normalizedVelocityPerSec: Float,
) {
    Slow("慢速", 0.09f),      // ~11s per character (1st-grade default)
    Normal("标准", 0.135f),   // ~7.4s per character
    Fast("挑战", 0.19f),      // ~5.2s per character
}

enum class ZiGamePhase {
    Lobby,
    Playing,
    Paused,
    Result,
}

sealed interface ZiVoiceModelState {
    data object Checking : ZiVoiceModelState
    data object NotDownloaded : ZiVoiceModelState
    data class Downloading(val progress: Float, val downloadedMb: Float, val totalMb: Float) : ZiVoiceModelState
    data object Ready : ZiVoiceModelState
    data class Error(val message: String) : ZiVoiceModelState
}

enum class ZiMissedSyncState {
    Idle,
    Syncing,
    Synced,
    Failed,
}

data class ZiFallingTile(
    val zi: String,
    val pinyin: String,
    /** Normalized horizontal center in [0.16f, 0.84f]. */
    val xFraction: Float,
    /** Normalized vertical progress: 0f = top edge, 1f = ground defense line. */
    val yFraction: Float,
    val wobbleTime: Float = 0f,
)

data class ZiFireworkParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val sizeDp: Float,
    val color: Color,
    val life: Float,
    val maxLife: Float,
    val gravity: Float = 0.72f,
)

data class ZiFloatingText(
    val x: Float,
    val y: Float,
    val text: String,
    val color: Color,
    val life: Float,
    val maxLife: Float = 1.0f,
)

data class ZiMissedChar(
    val zi: String,
    val pinyin: String,
    val selectedForReview: Boolean = true,
)

data class ZiGameUiState(
    val phase: ZiGamePhase = ZiGamePhase.Lobby,
    val selectedStage: ZiGameStage = ZiGameLevels.stageForNumber(1),
    val highestUnlockedStage: Int = 1,
    val stageStars: Map<Int, Int> = emptyMap(),
    val bestScore: Int = 0,
    val speed: ZiGameSpeed = ZiGameSpeed.Slow,
    val readablePoolSize: Int = 0,
    val isLoadingPool: Boolean = true,
    val poolLoadFailed: Boolean = false,
    val hasMicPermission: Boolean = false,
    val modelState: ZiVoiceModelState = ZiVoiceModelState.Checking,
    val isListening: Boolean = false,
    val micLevel: Float = 0f,
    val heardText: String = "",
    val feedbackBanner: String = "正在听你读字... 可以读单字或组词哦！",
    val feedbackKind: ZiFeedbackKind = ZiFeedbackKind.Listening,
    val totalCharsInStage: Int = 20,
    val completedCount: Int = 0,
    val hitCount: Int = 0,
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val currentTile: ZiFallingTile? = null,
    val particles: List<ZiFireworkParticle> = emptyList(),
    val floatingTexts: List<ZiFloatingText> = emptyList(),
    val shakeTime: Float = 0f,
    val groundFlashTime: Float = 0f,
    val missedChars: List<ZiMissedChar> = emptyList(),
    val missedSyncState: ZiMissedSyncState = ZiMissedSyncState.Idle,
    val newlyUnlockedStage: ZiGameStage? = null,
) {
    val accuracyPercent: Int
        get() = if (totalCharsInStage <= 0) 0 else ((hitCount * 100f) / totalCharsInStage).toInt()

    val earnedStars: Int
        get() = when {
            accuracyPercent >= 90 -> 3
            accuracyPercent >= 65 -> 2
            else -> 1
        }

    val selectedMissedCount: Int
        get() = missedChars.count { it.selectedForReview }

    val canStartGame: Boolean
        get() = readablePoolSize > 0 &&
            !isLoadingPool &&
            hasMicPermission &&
            modelState is ZiVoiceModelState.Ready
}

enum class ZiFeedbackKind {
    Listening,
    Hit,
    Missed,
    Muted,
}
