package app.tinks.tink.zi.game

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.network.ApiResult
import app.tinks.tink.zi.Zi
import app.tinks.tink.zi.isReadable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ZiGameSoundEffect {
    Firework,
    Miss,
    StageClear,
}

@HiltViewModel
class ZiGameViewModel @Inject constructor(
    private val characterSource: ZiGameCharacterDataSource,
    private val modelProvider: ZiVoiceModelProvider,
    private val speechEngine: ZiSpeechEngine,
    private val progressRepository: ZiGameProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState = _uiState.asStateFlow()

    private val _soundEffects = MutableSharedFlow<ZiGameSoundEffect>(extraBufferCapacity = 8)
    val soundEffects = _soundEffects.asSharedFlow()

    private var readablePool: List<String> = emptyList()
    private var stageQueue: List<String> = emptyList()
    private var nextSpawnDelay: Float = 0f
    private var poolJob: Job? = null
    private var modelJob: Job? = null
    private var syncJob: Job? = null
    private val random = Random.Default

    init {
        refreshPool()
        checkOrPrepareModel(autoDownload = false)
    }

    private fun initialUiState(): ZiGameUiState {
        val saved = progressRepository.load()
        val initialStage = ZiGameLevels.stageForNumber(saved.highestUnlockedStage)
        return ZiGameUiState(
            phase = ZiGamePhase.Lobby,
            selectedStage = initialStage,
            highestUnlockedStage = saved.highestUnlockedStage,
            stageStars = saved.stageStars,
            bestScore = saved.bestScore,
            speed = saved.speed,
        )
    }

    fun refreshPool() {
        poolJob?.cancel()
        poolJob = characterSource.getAllZis().onEach { result ->
            when (result) {
                is ApiResult.Loading -> _uiState.update {
                    it.copy(isLoadingPool = true, poolLoadFailed = false)
                }

                is ApiResult.Success -> {
                    val readable = result.data
                        .filter(Zi::isReadable)
                        .map { it.zi.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    readablePool = readable
                    _uiState.update {
                        it.copy(
                            isLoadingPool = false,
                            poolLoadFailed = false,
                            readablePoolSize = readable.size,
                            totalCharsInStage = minOf(CHARS_PER_STAGE, readable.size),
                        )
                    }
                }

                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoadingPool = false, poolLoadFailed = true)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun updateMicPermission(granted: Boolean) {
        _uiState.update { it.copy(hasMicPermission = granted) }
    }

    fun checkOrPrepareModel(autoDownload: Boolean = true) {
        if (modelProvider.isModelReady()) {
            _uiState.update { it.copy(modelState = ZiVoiceModelState.Ready) }
            return
        }
        if (!autoDownload) {
            _uiState.update { it.copy(modelState = ZiVoiceModelState.NotDownloaded) }
            return
        }
        if (modelJob?.isActive == true) return
        modelJob = modelProvider.ensureModelReady().onEach { modelState ->
            _uiState.update { it.copy(modelState = modelState) }
        }.launchIn(viewModelScope)
    }

    fun selectStage(stageNumber: Int) {
        val clamped = stageNumber.coerceIn(1, _uiState.value.highestUnlockedStage)
        _uiState.update {
            it.copy(selectedStage = ZiGameLevels.stageForNumber(clamped))
        }
    }

    fun selectSpeed(speed: ZiGameSpeed) {
        progressRepository.saveSpeed(speed)
        _uiState.update { it.copy(speed = speed) }
    }

    fun startGame() {
        val current = _uiState.value
        if (!current.canStartGame || readablePool.isEmpty()) return

        stageQueue = readablePool.shuffled(random).take(CHARS_PER_STAGE)
        nextSpawnDelay = 0f
        _uiState.update {
            it.copy(
                phase = ZiGamePhase.Playing,
                totalCharsInStage = stageQueue.size,
                completedCount = 0,
                hitCount = 0,
                score = 0,
                combo = 0,
                maxCombo = 0,
                currentTile = null,
                particles = emptyList(),
                floatingTexts = emptyList(),
                shakeTime = 0f,
                groundFlashTime = 0f,
                missedChars = emptyList(),
                missedSyncState = ZiMissedSyncState.Idle,
                newlyUnlockedStage = null,
                heardText = "",
                feedbackBanner = "正在听你读字... 可以读单字或组词哦！",
                feedbackKind = ZiFeedbackKind.Listening,
            )
        }
        spawnNextTile()
        startListening()
    }

    fun startNextStage() {
        val nextNumber = (_uiState.value.selectedStage.number + 1)
            .coerceAtMost(_uiState.value.highestUnlockedStage)
        selectStage(nextNumber)
        startGame()
    }

    fun pauseGame() {
        if (_uiState.value.phase != ZiGamePhase.Playing) return
        stopListening()
        _uiState.update { it.copy(phase = ZiGamePhase.Paused) }
    }

    fun resumeGame() {
        if (_uiState.value.phase != ZiGamePhase.Paused) return
        _uiState.update {
            it.copy(
                phase = ZiGamePhase.Playing,
                feedbackBanner = "正在听你读字... 可以读单字或组词哦！",
                feedbackKind = ZiFeedbackKind.Listening,
            )
        }
        startListening()
    }

    fun returnToLobby() {
        stopListening()
        _uiState.update {
            it.copy(
                phase = ZiGamePhase.Lobby,
                currentTile = null,
                particles = emptyList(),
                floatingTexts = emptyList(),
            )
        }
    }

    fun toggleListening() {
        if (_uiState.value.phase != ZiGamePhase.Playing) return
        if (_uiState.value.isListening) {
            stopListening()
            _uiState.update {
                it.copy(
                    feedbackBanner = "麦克风已暂停，点击麦克风恢复聆听",
                    feedbackKind = ZiFeedbackKind.Muted,
                )
            }
        } else {
            startListening()
            _uiState.update {
                it.copy(
                    feedbackBanner = "正在听你读字... 可以读单字或组词哦！",
                    feedbackKind = ZiFeedbackKind.Listening,
                )
            }
        }
    }

    fun onSpeechRecognized(spokenText: String) {
        val current = _uiState.value
        if (current.phase != ZiGamePhase.Playing) return
        val tile = current.currentTile ?: return
        val trimmed = spokenText.trim()
        if (trimmed.isEmpty()) return

        if (ZiPinyinMatcher.matchesSpokenText(tile.zi, trimmed, allowPhrase = true)) {
            onTileHit(tile, trimmed)
        } else {
            _uiState.update {
                it.copy(
                    heardText = trimmed,
                    currentTile = it.currentTile?.copy(wobbleTime = 0.28f),
                    feedbackBanner = "听到「$trimmed」不对哦，再大声读一次",
                    feedbackKind = ZiFeedbackKind.Listening,
                )
            }
        }
    }

    fun tick(dtSeconds: Float) {
        val dt = dtSeconds.coerceIn(0f, 0.05f)
        val current = _uiState.value
        if (current.phase != ZiGamePhase.Playing) return

        val updatedParticles = current.particles.mapNotNull { p ->
            val nextLife = p.life - dt
            if (nextLife <= 0f) null
            else {
                val nextVy = p.vy + p.gravity * dt
                p.copy(
                    x = p.x + p.vx * dt,
                    y = p.y + nextVy * dt,
                    vy = nextVy,
                    life = nextLife,
                )
            }
        }

        val updatedFloats = current.floatingTexts.mapNotNull { f ->
            val nextLife = f.life - dt
            if (nextLife <= 0f) null
            else f.copy(y = f.y - 0.10f * dt, life = nextLife)
        }

        val nextShake = (current.shakeTime - dt).coerceAtLeast(0f)
        val nextFlash = (current.groundFlashTime - dt).coerceAtLeast(0f)

        val tile = current.currentTile
        if (tile != null) {
            val nextY = tile.yFraction + current.speed.normalizedVelocityPerSec * dt
            val nextWobble = (tile.wobbleTime - dt).coerceAtLeast(0f)
            if (nextY >= 1.0f) {
                onTileMissed(tile, updatedParticles, updatedFloats)
            } else {
                _uiState.update {
                    it.copy(
                        currentTile = tile.copy(yFraction = nextY, wobbleTime = nextWobble),
                        particles = updatedParticles,
                        floatingTexts = updatedFloats,
                        shakeTime = nextShake,
                        groundFlashTime = nextFlash,
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    particles = updatedParticles,
                    floatingTexts = updatedFloats,
                    shakeTime = nextShake,
                    groundFlashTime = nextFlash,
                )
            }
            if (nextSpawnDelay > 0f) {
                nextSpawnDelay -= dt
                if (nextSpawnDelay <= 0f) {
                    spawnNextTile()
                }
            }
        }
    }

    fun toggleMissedSelection(zi: String) {
        val current = _uiState.value
        if (current.missedSyncState == ZiMissedSyncState.Syncing ||
            current.missedSyncState == ZiMissedSyncState.Synced
        ) return
        _uiState.update { state ->
            state.copy(
                missedChars = state.missedChars.map { item ->
                    if (item.zi == zi) item.copy(selectedForReview = !item.selectedForReview) else item
                },
            )
        }
    }

    fun syncMissedToReview() {
        val current = _uiState.value
        if (current.missedSyncState == ZiMissedSyncState.Syncing ||
            current.missedSyncState == ZiMissedSyncState.Synced
        ) return
        val zisToDowngrade = current.missedChars
            .filter { it.selectedForReview }
            .joinToString(separator = "") { it.zi }
        if (zisToDowngrade.isEmpty()) return

        syncJob?.cancel()
        syncJob = characterSource.downgradeMissedToReview(
            zis = zisToDowngrade,
            proficiency = REVIEW_PROFICIENCY_LEVEL,
        ).onEach { result ->
            when (result) {
                is ApiResult.Loading -> _uiState.update {
                    it.copy(missedSyncState = ZiMissedSyncState.Syncing)
                }

                is ApiResult.Success -> {
                    // Refresh readable pool so subsequent rounds reflect the downgraded characters.
                    refreshPool()
                    _uiState.update {
                        it.copy(missedSyncState = ZiMissedSyncState.Synced)
                    }
                }

                is ApiResult.Error -> _uiState.update {
                    it.copy(missedSyncState = ZiMissedSyncState.Failed)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun onTileHit(tile: ZiFallingTile, spokenDisplay: String) {
        speechEngine.resetUtterance()
        _soundEffects.tryEmit(ZiGameSoundEffect.Firework)

        val current = _uiState.value
        val nextCombo = current.combo + 1
        val nextMaxCombo = maxOf(current.maxCombo, nextCombo)
        val comboBonus = ((nextCombo - 1) * 10).coerceAtMost(50)
        val heightBonus = if (tile.yFraction < 0.35f) 20 else 0
        val gained = 100 + comboBonus + heightBonus

        val fireworks = createFireworkBurst(tile.xFraction, tile.yFraction)
        val label = "+$gained" + if (nextCombo >= 3) " x$nextCombo" else ""
        val floating = ZiFloatingText(
            x = tile.xFraction,
            y = (tile.yFraction - 0.04f).coerceAtLeast(0.08f),
            text = label,
            color = Color(0xFFFEF08A),
            life = 1.0f,
            maxLife = 1.0f,
        )

        val nextCompleted = current.completedCount + 1
        _uiState.update {
            it.copy(
                currentTile = null,
                completedCount = nextCompleted,
                hitCount = it.hitCount + 1,
                score = it.score + gained,
                combo = nextCombo,
                maxCombo = nextMaxCombo,
                particles = it.particles + fireworks,
                floatingTexts = it.floatingTexts + floating,
                shakeTime = 0.14f,
                heardText = spokenDisplay,
                feedbackBanner = "听到「$spokenDisplay」→ 读对 ${tile.zi} (${tile.pinyin})！",
                feedbackKind = ZiFeedbackKind.Hit,
            )
        }
        nextSpawnDelay = 0.55f
    }

    private fun onTileMissed(
        tile: ZiFallingTile,
        existingParticles: List<ZiFireworkParticle>,
        existingFloats: List<ZiFloatingText>,
    ) {
        speechEngine.resetUtterance()
        _soundEffects.tryEmit(ZiGameSoundEffect.Miss)

        val crashParticles = createGroundCrashParticles(tile.xFraction)
        val pinyinHint = if (tile.pinyin.isNotBlank()) "${tile.zi} · ${tile.pinyin}" else tile.zi
        val floating = ZiFloatingText(
            x = tile.xFraction,
            y = 0.88f,
            text = pinyinHint,
            color = Color(0xFFFDA4AF),
            life = 1.25f,
            maxLife = 1.25f,
        )

        val missedEntry = ZiMissedChar(
            zi = tile.zi,
            pinyin = tile.pinyin,
            selectedForReview = true,
        )

        _uiState.update {
            val updatedMissed = if (it.missedChars.any { m -> m.zi == tile.zi }) {
                it.missedChars
            } else {
                it.missedChars + missedEntry
            }
            it.copy(
                currentTile = null,
                completedCount = it.completedCount + 1,
                combo = 0,
                particles = existingParticles + crashParticles,
                floatingTexts = existingFloats + floating,
                shakeTime = 0.2f,
                groundFlashTime = 0.35f,
                missedChars = updatedMissed,
                feedbackBanner = "落地啦！正确读音：$pinyinHint",
                feedbackKind = ZiFeedbackKind.Missed,
            )
        }
        nextSpawnDelay = 0.72f
    }

    private fun spawnNextTile() {
        val index = _uiState.value.completedCount
        if (index >= stageQueue.size) {
            finishStage()
            return
        }
        val zi = stageQueue[index]
        val pinyin = ZiPinyinMatcher.displayPinyin(zi)
        val xFraction = 0.18f + random.nextFloat() * 0.64f
        speechEngine.resetUtterance()
        _uiState.update {
            it.copy(
                currentTile = ZiFallingTile(
                    zi = zi,
                    pinyin = pinyin,
                    xFraction = xFraction,
                    yFraction = 0f,
                ),
            )
        }
    }

    private fun finishStage() {
        stopListening()
        _soundEffects.tryEmit(ZiGameSoundEffect.StageClear)

        val current = _uiState.value
        val previousUnlocked = current.highestUnlockedStage
        val saved = progressRepository.recordStageResult(
            stageNumber = current.selectedStage.number,
            score = current.score,
            stars = current.earnedStars,
        )
        val newlyUnlocked = if (saved.highestUnlockedStage > previousUnlocked) {
            ZiGameLevels.stageForNumber(saved.highestUnlockedStage)
        } else {
            null
        }

        _uiState.update {
            it.copy(
                phase = ZiGamePhase.Result,
                currentTile = null,
                highestUnlockedStage = saved.highestUnlockedStage,
                stageStars = saved.stageStars,
                bestScore = saved.bestScore,
                newlyUnlockedStage = newlyUnlocked,
            )
        }
    }

    private fun startListening() {
        val files = modelProvider.modelFilesOrNull() ?: return
        _uiState.update { it.copy(isListening = true) }
        speechEngine.start(
            modelFiles = files,
            onPartialText = { text -> onSpeechRecognized(text) },
            onMicLevel = { level -> _uiState.update { it.copy(micLevel = level) } },
            onError = { errorMsg ->
                _uiState.update {
                    it.copy(
                        isListening = false,
                        micLevel = 0f,
                        feedbackBanner = errorMsg,
                        feedbackKind = ZiFeedbackKind.Missed,
                    )
                }
            },
        )
    }

    private fun stopListening() {
        speechEngine.stop()
        _uiState.update { it.copy(isListening = false, micLevel = 0f) }
    }

    private fun createFireworkBurst(cx: Float, cy: Float): List<ZiFireworkParticle> {
        val palette = listOf(
            Color(0xFFFACC15),
            Color(0xFFFB7185),
            Color(0xFF38BDF8),
            Color(0xFF4ADE80),
            Color(0xFFC084FC),
            Color(0xFFFFFFFF),
            Color(0xFFFB923C),
        )
        val count = 36
        return List(count) { idx ->
            val angle = (2.0 * PI * idx / count) + (random.nextDouble() - 0.5) * 0.25
            val speed = 0.22f + random.nextFloat() * 0.45f
            val life = 0.65f + random.nextFloat() * 0.45f
            ZiFireworkParticle(
                x = cx,
                y = cy,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat() - 0.14f,
                sizeDp = if (idx % 2 == 0) 6f else 4f,
                color = palette[idx % palette.size],
                life = life,
                maxLife = life,
            )
        }
    }

    private fun createGroundCrashParticles(cx: Float): List<ZiFireworkParticle> {
        val colors = listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFF94A3B8), Color(0xFF475569))
        return List(16) { idx ->
            val life = 0.45f + random.nextFloat() * 0.25f
            ZiFireworkParticle(
                x = (cx + (random.nextFloat() - 0.5f) * 0.12f).coerceIn(0.05f, 0.95f),
                y = 0.96f,
                vx = (random.nextFloat() - 0.5f) * 0.35f,
                vy = -(0.12f + random.nextFloat() * 0.28f),
                sizeDp = 5f,
                color = colors[idx % colors.size],
                life = life,
                maxLife = life,
                gravity = 0.85f,
            )
        }
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }

    companion object {
        const val CHARS_PER_STAGE = 20
        const val REVIEW_PROFICIENCY_LEVEL = 3
    }
}
