package app.tinks.tink.zi.game

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private val PixelBgDeep = Color(0xFF0B0E23)
private val PixelPanel = Color(0xFF171C3A)
private val PixelHudBg = Color(0xFF121633)
private val PixelBorderIndigo = Color(0xFF6366F1)
private val PixelBorderCyan = Color(0xFF38BDF8)
private val PixelBorderSlate = Color(0xFF475569)
private val PixelGold = Color(0xFFFACC15)
private val PixelGoldLight = Color(0xFFFEF08A)
private val PixelGreen = Color(0xFF22C55E)
private val PixelGreenLight = Color(0xFFBBF7D0)
private val PixelGreenDark = Color(0xFF052E16)
private val PixelRose = Color(0xFFF43F5E)
private val PixelRoseLight = Color(0xFFFECDD3)
private val PixelOrange = Color(0xFFF97316)
private val PixelRed = Color(0xFFEF4444)
private val PixelTextMuted = Color(0xFF94A3B8)
private val PixelTextLight = Color(0xFFE2E8F0)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ZiGameRoute(
    viewModel: ZiGameViewModel,
    onExit: (syncedMissedChars: Boolean) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val audio = remember(context) { ZiGameAudio(context.applicationContext) }
    DisposableEffect(audio) {
        onDispose { audio.close() }
    }

    LaunchedEffect(viewModel, audio) {
        viewModel.soundEffects.collect { effect ->
            when (effect) {
                ZiGameSoundEffect.Firework -> audio.playFirework()
                ZiGameSoundEffect.Miss -> audio.playMiss()
                ZiGameSoundEffect.StageClear -> audio.playStageClear()
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.updateMicPermission(granted)
    }

    LaunchedEffect(context) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updateMicPermission(granted)
        if (!granted) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Lock portrait orientation and keep screen awake while playing.
    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.requestedOrientation = previousOrientation
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Auto-pause game when app leaves foreground.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.pauseGame()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                viewModel.updateMicPermission(granted)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // 60fps physics & particle game loop.
    LaunchedEffect(state.phase) {
        if (state.phase != ZiGamePhase.Playing) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameNanos != 0L) {
                    val dt = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                    viewModel.tick(dt)
                }
                lastFrameNanos = frameTimeNanos
            }
        }
    }

    BackHandler {
        when (state.phase) {
            ZiGamePhase.Playing -> viewModel.pauseGame()
            ZiGamePhase.Paused -> viewModel.resumeGame()
            ZiGamePhase.Lobby, ZiGamePhase.Result -> {
                onExit(state.missedSyncState == ZiMissedSyncState.Synced)
            }
        }
    }

    ZiGameScreen(
        state = state,
        onBack = { onExit(state.missedSyncState == ZiMissedSyncState.Synced) },
        onSelectStage = viewModel::selectStage,
        onSelectSpeed = viewModel::selectSpeed,
        onRequestMicPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onDownloadModel = { viewModel.checkOrPrepareModel(autoDownload = true) },
        onRetryPool = viewModel::refreshPool,
        onStartGame = viewModel::startGame,
        onStartNextStage = viewModel::startNextStage,
        onPause = viewModel::pauseGame,
        onResume = viewModel::resumeGame,
        onReturnToLobby = viewModel::returnToLobby,
        onToggleMic = viewModel::toggleListening,
        onToggleMissedChar = viewModel::toggleMissedSelection,
        onSyncMissedChars = viewModel::syncMissedToReview,
    )
}

@Composable
internal fun ZiGameScreen(
    state: ZiGameUiState,
    onBack: () -> Unit = {},
    onSelectStage: (Int) -> Unit = {},
    onSelectSpeed: (ZiGameSpeed) -> Unit = {},
    onRequestMicPermission: () -> Unit = {},
    onDownloadModel: () -> Unit = {},
    onRetryPool: () -> Unit = {},
    onStartGame: () -> Unit = {},
    onStartNextStage: () -> Unit = {},
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onReturnToLobby: () -> Unit = {},
    onToggleMic: () -> Unit = {},
    onToggleMissedChar: (String) -> Unit = {},
    onSyncMissedChars: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelBgDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("zi_game_root"),
    ) {
        when (state.phase) {
            ZiGamePhase.Lobby -> ZiGameLobby(
                state = state,
                onBack = onBack,
                onSelectStage = onSelectStage,
                onSelectSpeed = onSelectSpeed,
                onRequestMicPermission = onRequestMicPermission,
                onDownloadModel = onDownloadModel,
                onRetryPool = onRetryPool,
                onStartGame = onStartGame,
            )

            ZiGamePhase.Playing, ZiGamePhase.Paused -> {
                ZiGamePlayfield(
                    state = state,
                    onPause = onPause,
                    onToggleMic = onToggleMic,
                )
                if (state.phase == ZiGamePhase.Paused) {
                    ZiGamePauseModal(
                        stage = state.selectedStage,
                        onResume = onResume,
                        onRestart = onStartGame,
                        onQuitToLobby = onReturnToLobby,
                    )
                }
            }

            ZiGamePhase.Result -> ZiGameResult(
                state = state,
                onToggleMissedChar = onToggleMissedChar,
                onSyncMissedChars = onSyncMissedChars,
                onNextStage = onStartNextStage,
                onReplay = onStartGame,
                onExit = onBack,
            )
        }
    }
}

@Composable
private fun ZiGameLobby(
    state: ZiGameUiState,
    onBack: () -> Unit,
    onSelectStage: (Int) -> Unit,
    onSelectSpeed: (ZiGameSpeed) -> Unit,
    onRequestMicPermission: () -> Unit,
    onDownloadModel: () -> Unit,
    onRetryPool: () -> Unit,
    onStartGame: () -> Unit,
) {
    val stages = remember(state.highestUnlockedStage) {
        ZiGameLevels.visibleStages(state.highestUnlockedStage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("zi_game_lobby"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelButton(
                text = "◀ 返回",
                background = Color(0xFF1E2448),
                borderColor = PixelBorderSlate,
                contentColor = PixelTextLight,
                compact = true,
                onClick = onBack,
                modifier = Modifier.testTag("zi_game_back_button"),
            )
            Text(
                text = "最高分 ${state.bestScore}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = PixelGold,
            )
        }

        // Hero Banner with selected Solar System planet / asteroid
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFEC4899))
                    .border(2.dp, Color(0xFFFBCFE8))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = state.selectedStage.kindLabel,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    color = Color.White,
                )
            }
            Text(
                text = "汉字星空防线",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = PixelGoldLight,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                PixelPlanetIcon(stage = state.selectedStage, size = 44.dp)
                Column {
                    Text(
                        text = state.selectedStage.displayTitle,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.testTag("zi_game_selected_stage_title"),
                    )
                    Text(
                        text = "大声读出掉落汉字 · 炸出像素烟花！",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = PixelTextMuted,
                    )
                }
            }
        }

        // Solar System Level Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "太阳系航线选关",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PixelTextLight,
                )
                Text(
                    text = "已解锁 ${state.highestUnlockedStage} 站",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = PixelBorderCyan,
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
                modifier = Modifier.testTag("zi_game_stage_picker"),
            ) {
                items(stages, key = { it.number }) { stage ->
                    val unlocked = stage.number <= state.highestUnlockedStage
                    val selected = stage.number == state.selectedStage.number
                    val stars = state.stageStars[stage.number] ?: 0
                    SolarStageChip(
                        stage = stage,
                        unlocked = unlocked,
                        selected = selected,
                        stars = stars,
                        onClick = { if (unlocked) onSelectStage(stage.number) },
                    )
                }
            }
        }

        // Settings Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelPanel)
                .border(3.dp, PixelBorderIndigo)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "题库来源",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PixelTextLight,
                )
                when {
                    state.isLoadingPool -> Text(
                        text = "正在加载会读汉字...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = PixelBorderCyan,
                    )
                    state.poolLoadFailed -> PixelButton(
                        text = "加载失败 · 重试",
                        background = PixelRose,
                        borderColor = PixelRoseLight,
                        contentColor = Color.White,
                        compact = true,
                        onClick = onRetryPool,
                    )
                    state.readablePoolSize == 0 -> Text(
                        text = "暂无会读汉字（需熟练度5）",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = PixelGold,
                    )
                    else -> Text(
                        text = "会读 ${state.readablePoolSize}字 → 抽${state.totalCharsInStage}字",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PixelBorderCyan,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "掉落速度",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PixelTextLight,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ZiGameSpeed.entries.forEach { speed ->
                        val selected = state.speed == speed
                        Box(
                            modifier = Modifier
                                .background(if (selected) PixelBorderCyan else Color(0xFF0F172A))
                                .border(
                                    2.dp,
                                    if (selected) Color(0xFFBAE6FD) else PixelBorderSlate,
                                )
                                .clickable { onSelectSpeed(speed) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("zi_game_speed_${speed.name.lowercase()}"),
                        ) {
                            Text(
                                text = if (speed == ZiGameSpeed.Slow) "慢速(推荐)" else speed.label,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (selected) Color(0xFF082F49) else PixelTextMuted,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "判定方式",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PixelTextLight,
                )
                Text(
                    text = "宽松拼音 · 支持组词读",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF86EFAC),
                )
            }
        }

        // Mic & Offline Voice Model Status Box
        VoiceEngineStatusCard(
            hasMicPermission = state.hasMicPermission,
            modelState = state.modelState,
            onRequestMicPermission = onRequestMicPermission,
            onDownloadModel = onDownloadModel,
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))

        // Primary Start CTA
        val ctaLabel = when {
            !state.hasMicPermission -> "🔒 请先授权麦克风"
            state.modelState is ZiVoiceModelState.NotDownloaded -> "⬇ 下载离线语音包 (~25MB)"
            state.modelState is ZiVoiceModelState.Downloading -> "⏳ 离线语音包准备中..."
            state.modelState is ZiVoiceModelState.Error -> "↻ 重新下载离线语音包"
            state.readablePoolSize == 0 -> "请先在识字页添加会读汉字"
            else -> "▶ 开始闯关 · ${state.selectedStage.name} (${state.totalCharsInStage}字)"
        }

        val ctaAction: () -> Unit = when {
            !state.hasMicPermission -> onRequestMicPermission
            state.modelState is ZiVoiceModelState.NotDownloaded ||
                state.modelState is ZiVoiceModelState.Error -> onDownloadModel
            else -> onStartGame
        }

        val ctaEnabled = when {
            !state.hasMicPermission -> true
            state.modelState is ZiVoiceModelState.NotDownloaded ||
                state.modelState is ZiVoiceModelState.Error -> true
            else -> state.canStartGame
        }

        PixelButton(
            text = ctaLabel,
            background = if (state.canStartGame) PixelGreen else PixelGold,
            borderColor = if (state.canStartGame) PixelGreenLight else PixelGoldLight,
            contentColor = PixelGreenDark,
            enabled = ctaEnabled,
            onClick = ctaAction,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("zi_game_start_button"),
        )
    }
}

@Composable
private fun SolarStageChip(
    stage: ZiGameStage,
    unlocked: Boolean,
    selected: Boolean,
    stars: Int,
    onClick: () -> Unit,
) {
    val bg = when {
        selected -> Color(0xFF232C5E)
        unlocked -> PixelPanel
        else -> Color(0xFF0F1326)
    }
    val border = when {
        selected -> PixelGold
        unlocked -> PixelBorderIndigo
        else -> Color(0xFF334155)
    }
    Column(
        modifier = Modifier
            .width(84.dp)
            .background(bg)
            .border(if (selected) 3.dp else 2.dp, border)
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag("zi_game_stage_${stage.number}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "第${stage.number}关",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = if (selected) PixelGoldLight else PixelTextMuted,
        )
        Box(contentAlignment = Alignment.Center) {
            PixelPlanetIcon(
                stage = stage,
                size = 30.dp,
                dimmed = !unlocked,
            )
            if (!unlocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "未解锁",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = stage.name,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (unlocked) Color.White else PixelTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            repeat(3) { idx ->
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (idx < stars) PixelGold else Color(0xFF334155),
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}

@Composable
private fun VoiceEngineStatusCard(
    hasMicPermission: Boolean,
    modelState: ZiVoiceModelState,
    onRequestMicPermission: () -> Unit,
    onDownloadModel: () -> Unit,
) {
    val (borderColor, bgColor, textColor, message) = when {
        !hasMicPermission -> Quad(
            PixelRed,
            Color(0xFF2A0F14),
            Color(0xFFFCA5A5),
            "需要麦克风权限才能听懂小朋友读字",
        )
        modelState is ZiVoiceModelState.Downloading -> Quad(
            PixelBorderCyan,
            Color(0xFF0F172A),
            Color(0xFFBAE6FD),
            "正在下载离线中文语音包 (${"%.1f".format(modelState.downloadedMb)}/${"%.0f".format(modelState.totalMb)} MB)...",
        )
        modelState is ZiVoiceModelState.NotDownloaded -> Quad(
            PixelGold,
            Color(0xFF271B08),
            PixelGoldLight,
            "首次使用需下载离线中文语音包 (~25MB，免Google服务)",
        )
        modelState is ZiVoiceModelState.Error -> Quad(
            PixelRed,
            Color(0xFF2A0F14),
            Color(0xFFFCA5A5),
            modelState.message,
        )
        modelState is ZiVoiceModelState.Checking -> Quad(
            PixelBorderSlate,
            Color(0xFF0F172A),
            PixelTextMuted,
            "正在检查离线语音引擎...",
        )
        else -> Quad(
            PixelGreen,
            Color(0xFF0F172A),
            Color(0xFF86EFAC),
            "离线语音引擎已就绪 · 无需Google语音服务",
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .border(2.dp, borderColor)
            .padding(10.dp)
            .testTag("zi_game_voice_status"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = textColor,
                modifier = Modifier.weight(1f),
            )
            if (!hasMicPermission) {
                Spacer(Modifier.width(8.dp))
                PixelButton(
                    text = "去授权",
                    background = PixelRose,
                    borderColor = PixelRoseLight,
                    contentColor = Color.White,
                    compact = true,
                    onClick = onRequestMicPermission,
                )
            } else if (modelState is ZiVoiceModelState.NotDownloaded || modelState is ZiVoiceModelState.Error) {
                Spacer(Modifier.width(8.dp))
                PixelButton(
                    text = if (modelState is ZiVoiceModelState.Error) "重试" else "下载",
                    background = PixelBorderCyan,
                    borderColor = Color(0xFFBAE6FD),
                    contentColor = Color(0xFF082F49),
                    compact = true,
                    onClick = onDownloadModel,
                )
            }
        }
        if (modelState is ZiVoiceModelState.Downloading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(PixelBgDeep)
                    .border(2.dp, PixelBorderCyan)
                    .padding(1.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(modelState.progress.coerceIn(0.05f, 1f))
                        .background(PixelBorderCyan),
                )
            }
        }
    }
}

@Composable
private fun ZiGamePlayfield(
    state: ZiGameUiState,
    onPause: () -> Unit,
    onToggleMic: () -> Unit,
) {
    // Lightweight tick for blinking stars & danger border pulse.
    var pulseMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.phase) {
        if (state.phase != ZiGamePhase.Playing) return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (true) {
            withFrameNanos {
                pulseMs = System.currentTimeMillis() - start
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("zi_game_playfield"),
    ) {
        // TOP HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelHudBg)
                .border(width = 2.dp, color = Color(0xFF334155))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF1E2448))
                            .border(2.dp, PixelBorderSlate)
                            .clickable(onClick = onPause)
                            .testTag("zi_game_pause_button"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "暂停",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color(0xFF1E2448))
                            .border(2.dp, state.selectedStage.primaryColor)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        PixelPlanetIcon(stage = state.selectedStage, size = 14.dp)
                        Text(
                            text = state.selectedStage.name,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.combo >= 2) {
                        Box(
                            modifier = Modifier
                                .background(PixelOrange)
                                .border(2.dp, Color(0xFFFED7AA))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "x${state.combo}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = Color.White,
                            )
                        }
                    }
                    Text(
                        text = state.score.toString().padStart(4, '0'),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = PixelGold,
                        modifier = Modifier.testTag("zi_game_score"),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val displayIndex = (state.completedCount + if (state.currentTile != null) 1 else 0)
                    .coerceAtMost(state.totalCharsInStage.coerceAtLeast(1))
                Text(
                    text = "$displayIndex/${state.totalCharsInStage}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White,
                    modifier = Modifier.testTag("zi_game_progress_text"),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .background(PixelBgDeep)
                        .border(2.dp, PixelBorderSlate)
                        .padding(1.dp),
                ) {
                    val progress = if (state.totalCharsInStage > 0) {
                        state.completedCount.toFloat() / state.totalCharsInStage.toFloat()
                    } else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(PixelBorderCyan),
                    )
                }
                Text(
                    text = "✓${state.hitCount}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF4ADE80),
                )
            }
        }

        // MAIN PIXEL CANVAS STAGE
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(PixelBgDeep),
        ) {
            PixelGameCanvas(
                state = state,
                pulseMs = pulseMs,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // BOTTOM MICROPHONE DOCK
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelHudBg)
                .border(width = 2.dp, color = PixelBorderCyan)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val (tagText, tagBg, tagColor) = when (state.feedbackKind) {
                ZiFeedbackKind.Hit -> Triple("HIT ✓", Color(0xFF14532D), Color(0xFF86EFAC))
                ZiFeedbackKind.Missed -> Triple("MISSED", Color(0xFF450A0A), Color(0xFFFCA5A5))
                ZiFeedbackKind.Muted -> Triple("MUTED", Color(0xFF334155), PixelTextMuted)
                ZiFeedbackKind.Listening -> Triple("LISTENING", Color(0xFF1E293B), PixelBorderCyan)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF090D20))
                    .border(2.dp, Color(0xFF334155))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = state.feedbackBanner,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = PixelTextLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("zi_game_feedback_banner"),
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(tagBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = tagText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        color = tagColor,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (state.isListening) PixelRed else PixelBorderSlate)
                        .border(3.dp, if (state.isListening) Color(0xFFFECACA) else PixelTextMuted)
                        .clickable(onClick = onToggleMic)
                        .testTag("zi_game_mic_toggle"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.isListening) Icons.Filled.Mic else Icons.Filled.MicOff,
                        contentDescription = if (state.isListening) "静音麦克风" else "开启麦克风",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // 14-bar pixel audio visualizer
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    val timeSec = pulseMs / 1000f
                    repeat(14) { idx ->
                        val activeFraction = if (!state.isListening) {
                            0.14f
                        } else {
                            val idleWave = 0.22f + 0.16f * abs(sin(timeSec * 6f + idx * 0.65f))
                            val boost = state.micLevel * (0.45f + 0.55f * abs(sin(timeSec * 12f + idx * 0.9f)))
                            (idleWave + boost).coerceIn(0.16f, 1f)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(activeFraction)
                                .background(
                                    when {
                                        !state.isListening -> PixelBorderSlate
                                        idx % 3 == 0 -> Color(0xFF4ADE80)
                                        else -> PixelBorderCyan
                                    },
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelGameCanvas(
    state: ZiGameUiState,
    pulseMs: Long,
    modifier: Modifier = Modifier,
) {
    val stars = remember {
        List(36) { idx ->
            Triple(
                ((idx * 37 + 13) % 97) / 100f,
                ((idx * 53 + 7) % 79) / 100f,
                idx,
            )
        }
    }
    val textPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    val floatPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.90f
        val timeSec = pulseMs / 1000f

        val shakeX = if (state.shakeTime > 0f) sin(timeSec * 90f) * 6f else 0f

        // Stepped pixel night-sky bands
        val skyColors = listOf(
            Color(0xFF090C1F),
            Color(0xFF0D122D),
            Color(0xFF11183C),
            Color(0xFF16204C),
        )
        val bandH = groundY / skyColors.size
        skyColors.forEachIndexed { idx, col ->
            drawRect(
                color = col,
                topLeft = Offset(0f, idx * bandH),
                size = Size(w, bandH + 1f),
            )
        }

        // Twinkling pixel stars
        val starColors = listOf(Color.White, Color(0xFFBAE6FD), PixelGoldLight, Color(0xFFC7D2FE))
        stars.forEach { (sx, sy, idx) ->
            val alpha = 0.35f + 0.65f * abs(sin(timeSec * 2.2f + idx))
            val pxSize = if (idx % 4 == 0) 4.dp.toPx() else 2.dp.toPx()
            drawRect(
                color = starColors[idx % starColors.size].copy(alpha = alpha),
                topLeft = Offset(sx * w, sy * groundY),
                size = Size(pxSize, pxSize),
            )
        }

        // Decorative pixel planet in upper-right sky
        drawMiniPlanetInSky(
            stage = state.selectedStage,
            center = Offset(w * 0.85f, h * 0.11f),
            radius = 18.dp.toPx(),
        )

        // Ground warning dashed line + pixel grass + brick defense wall
        val flash = state.groundFlashTime > 0f
        val dashW = 8.dp.toPx()
        var dx = 0f
        while (dx < w) {
            drawRect(
                color = if (flash) PixelRed else Color(0x73F87171),
                topLeft = Offset(dx, groundY - 3.dp.toPx()),
                size = Size(dashW, 2.dp.toPx()),
            )
            dx += dashW * 2f
        }
        drawRect(
            color = if (flash) Color(0xFFF87171) else PixelGreen,
            topLeft = Offset(0f, groundY),
            size = Size(w, 6.dp.toPx()),
        )
        drawRect(
            color = Color(0xFF334155),
            topLeft = Offset(0f, groundY + 6.dp.toPx()),
            size = Size(w, h - groundY - 6.dp.toPx()),
        )
        val brickW = 26.dp.toPx()
        val brickH = 10.dp.toPx()
        for (row in 0..2) {
            val by = groundY + 10.dp.toPx() + row * (brickH + 3.dp.toPx())
            if (by > h) break
            val offset = if (row % 2 == 0) 0f else brickW / 2f
            var bx = -brickW
            while (bx < w + brickW) {
                drawRect(
                    color = Color(0xFF475569),
                    topLeft = Offset(bx + offset, by),
                    size = Size(brickW - 3.dp.toPx(), brickH),
                )
                bx += brickW
            }
        }

        // Falling Hanzi tile
        state.currentTile?.let { tile ->
            val tileSize = 84.dp.toPx()
            val danger = tile.yFraction > 0.72f
            val blinkRed = danger && ((pulseMs / 110L) % 2L == 0L)
            val wobbleOffset = when {
                tile.wobbleTime > 0f -> sin(tile.wobbleTime * 55f) * 7.dp.toPx()
                danger -> sin(timeSec * 36f) * 2.dp.toPx()
                else -> 0f
            }

            val cx = (tile.xFraction * w).coerceIn(tileSize * 0.55f, w - tileSize * 0.55f) + wobbleOffset + shakeX
            val bottomY = tile.yFraction * groundY
            val topY = bottomY - tileSize
            val leftX = cx - tileSize / 2f

            // Subtle falling pixel trails
            val trailColor = if (danger) Color(0x70F87171) else Color(0x5538BDF8)
            val trailW = 2.dp.toPx()
            drawRect(trailColor, Offset(leftX + tileSize * 0.2f, topY - 10.dp.toPx()), Size(trailW, 8.dp.toPx()))
            drawRect(trailColor, Offset(cx - trailW / 2f, topY - 18.dp.toPx()), Size(trailW, 14.dp.toPx()))
            drawRect(trailColor, Offset(leftX + tileSize * 0.8f, topY - 12.dp.toPx()), Size(trailW, 10.dp.toPx()))

            // Pixel drop shadow
            drawRect(
                color = Color(0xFF050712),
                topLeft = Offset(leftX + 4.dp.toPx(), topY + 6.dp.toPx()),
                size = Size(tileSize, tileSize),
            )

            val borderCol = when {
                blinkRed -> PixelRed
                danger -> PixelOrange
                else -> PixelBorderCyan
            }
            val innerCol = when {
                blinkRed -> Color(0xFF450A0A)
                danger -> Color(0xFF2A160C)
                else -> Color(0xFF172042)
            }
            val hiCol = when {
                blinkRed -> Color(0xFFFCA5A5)
                danger -> Color(0xFFFDBA74)
                else -> Color(0xFFBAE6FD)
            }

            val borderPx = 4.dp.toPx()
            drawRect(borderCol, Offset(leftX, topY), Size(tileSize, tileSize))
            drawRect(innerCol, Offset(leftX + borderPx, topY + borderPx), Size(tileSize - borderPx * 2, tileSize - borderPx * 2))
            drawRect(hiCol, Offset(leftX + borderPx + 2.dp.toPx(), topY + borderPx + 2.dp.toPx()), Size(tileSize - borderPx * 2 - 4.dp.toPx(), 2.dp.toPx()))

            // High-contrast crisp Hanzi glyph inside the pixel tile
            textPaint.textSize = 48.sp.toPx()
            val fontMetrics = textPaint.fontMetrics
            val textBaseline = topY + tileSize / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
            drawContext.canvas.nativeCanvas.drawText(tile.zi, cx, textBaseline, textPaint)
        }

        // Pixel firework & crash particles
        state.particles.forEach { p ->
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            val px = p.sizeDp.dp.toPx()
            drawRect(
                color = p.color.copy(alpha = alpha),
                topLeft = Offset(p.x * w + shakeX - px / 2f, p.y * groundY - px / 2f),
                size = Size(px, px),
            )
        }

        // Floating score / pinyin hints
        state.floatingTexts.forEach { ft ->
            val alpha = ((ft.life / ft.maxLife).coerceIn(0f, 1f) * 255).roundToInt()
            val fx = (ft.x * w).coerceIn(56.dp.toPx(), w - 56.dp.toPx())
            val fy = (ft.y * groundY).coerceIn(28.dp.toPx(), groundY - 12.dp.toPx())
            floatPaint.textSize = 18.sp.toPx()
            floatPaint.color = android.graphics.Color.argb(alpha, 9, 12, 28)
            drawContext.canvas.nativeCanvas.drawText(ft.text, fx + 2.dp.toPx(), fy + 2.dp.toPx(), floatPaint)
            val c = ft.color
            floatPaint.color = android.graphics.Color.argb(
                alpha,
                (c.red * 255).roundToInt(),
                (c.green * 255).roundToInt(),
                (c.blue * 255).roundToInt(),
            )
            drawContext.canvas.nativeCanvas.drawText(ft.text, fx, fy, floatPaint)
        }
    }
}

private fun DrawScope.drawMiniPlanetInSky(
    stage: ZiGameStage,
    center: Offset,
    radius: Float,
) {
    val px = (radius / 5f).coerceAtLeast(2f)
    val base = stage.primaryColor.copy(alpha = 0.32f)
    val hi = stage.accentColor.copy(alpha = 0.42f)
    // Stepped pixel circle approximation
    for (dy in -4..4) {
        for (dx in -4..4) {
            if (dx * dx + dy * dy <= 18) {
                drawRect(
                    color = if (dx < 0 && dy < 0) hi else base,
                    topLeft = Offset(center.x + dx * px, center.y + dy * px),
                    size = Size(px, px),
                )
            }
        }
    }
    if (stage.hasRing) {
        drawRect(
            color = hi,
            topLeft = Offset(center.x - radius * 1.45f, center.y - px / 2f),
            size = Size(radius * 2.9f, px),
        )
    }
}

@Composable
private fun ZiGamePauseModal(
    stage: ZiGameStage,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuitToLobby: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD2050816))
            .padding(24.dp)
            .testTag("zi_game_pause_modal"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelPanel)
                .border(3.dp, PixelBorderIndigo)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "游戏暂停",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = PixelGoldLight,
            )
            Text(
                text = "${stage.displayTitle} · 麦克风监听已暂停",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = PixelTextMuted,
            )
            PixelButton(
                text = "▶ 继续闯关",
                background = PixelGreen,
                borderColor = PixelGreenLight,
                contentColor = PixelGreenDark,
                onClick = onResume,
                modifier = Modifier.fillMaxWidth().testTag("zi_game_resume_button"),
            )
            PixelButton(
                text = "↻ 重新开始本关",
                background = Color(0xFF3B82F6),
                borderColor = Color(0xFFBFDBFE),
                contentColor = Color.White,
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
            )
            PixelButton(
                text = "◀ 返回太阳系选关",
                background = Color(0xFF1E2448),
                borderColor = PixelBorderSlate,
                contentColor = PixelTextLight,
                onClick = onQuitToLobby,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZiGameResult(
    state: ZiGameUiState,
    onToggleMissedChar: (String) -> Unit,
    onSyncMissedChars: () -> Unit,
    onNextStage: () -> Unit,
    onReplay: () -> Unit,
    onExit: () -> Unit,
) {
    val nextStage = ZiGameLevels.stageForNumber(state.selectedStage.number + 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("zi_game_result"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            repeat(3) { idx ->
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (idx < state.earnedStars) PixelGold else Color(0xFF334155),
                    modifier = Modifier.size(34.dp),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PixelPlanetIcon(stage = state.selectedStage, size = 32.dp)
            Text(
                text = "${state.selectedStage.name} 探索完成！",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = PixelGoldLight,
            )
        }

        state.newlyUnlockedStage?.let { unlocked ->
            Box(
                modifier = Modifier
                    .background(Color(0xFF14532D))
                    .border(2.dp, Color(0xFF86EFAC))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "🚀 已解锁新关卡：${unlocked.displayTitle}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFFDCFCE7),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ResultStatCell("本关积分", state.score.toString(), PixelGold, Modifier.weight(1f))
            ResultStatCell("读对比例", "${state.accuracyPercent}%", Color(0xFF4ADE80), Modifier.weight(1f))
            ResultStatCell("最高连击", "x${state.maxCombo}", PixelBorderCyan, Modifier.weight(1f))
        }

        // Missed characters & review sync card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelPanel)
                .border(3.dp, PixelBorderIndigo)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "未读出的字 · 错字联动",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = Color(0xFFFCA5A5),
                )
                Text(
                    text = "${state.missedChars.size} 字",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFFFCA5A5),
                )
            }

            if (state.missedChars.isEmpty()) {
                Text(
                    text = "🎉 太厉害了！本关全部汉字一次读对，没有错字！",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF86EFAC),
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            } else {
                Text(
                    text = "默认全选错字，点字可取消勾选（排除环境噪音误判）",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = PixelTextMuted,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("zi_game_missed_chips"),
                ) {
                    state.missedChars.forEach { missed ->
                        val checked = missed.selectedForReview
                        Row(
                            modifier = Modifier
                                .background(if (checked) Color(0xFF1E1B4B) else Color(0xFF0F172A))
                                .border(2.dp, if (checked) PixelRose else PixelBorderSlate)
                                .clickable(
                                    enabled = state.missedSyncState != ZiMissedSyncState.Syncing &&
                                        state.missedSyncState != ZiMissedSyncState.Synced,
                                ) { onToggleMissedChar(missed.zi) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("zi_game_missed_${missed.zi}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                text = if (checked) "☑" else "☐",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (checked) Color(0xFFFDA4AF) else PixelTextMuted,
                            )
                            Text(
                                text = missed.zi,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = if (checked) Color.White else PixelTextMuted,
                            )
                            if (missed.pinyin.isNotBlank()) {
                                Text(
                                    text = missed.pinyin,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (checked) Color(0xFFFDA4AF) else PixelTextMuted,
                                )
                            }
                        }
                    }
                }

                val syncButtonText = when (state.missedSyncState) {
                    ZiMissedSyncState.Idle ->
                        if (state.selectedMissedCount > 0) "将 ${state.selectedMissedCount} 个错字加入「复习汉字」"
                        else "未勾选需要复习的错字"
                    ZiMissedSyncState.Syncing -> "正在同步到复习汉字..."
                    ZiMissedSyncState.Synced -> "✓ 已将 ${state.selectedMissedCount} 个错字加入「复习汉字」"
                    ZiMissedSyncState.Failed -> "同步失败，点击重试"
                }

                PixelButton(
                    text = syncButtonText,
                    background = when (state.missedSyncState) {
                        ZiMissedSyncState.Synced -> PixelGreen
                        ZiMissedSyncState.Idle, ZiMissedSyncState.Failed ->
                            if (state.selectedMissedCount > 0) PixelRose else Color(0xFF1E2448)
                        ZiMissedSyncState.Syncing -> PixelBorderSlate
                    },
                    borderColor = when (state.missedSyncState) {
                        ZiMissedSyncState.Synced -> PixelGreenLight
                        else -> PixelRoseLight
                    },
                    contentColor = if (state.missedSyncState == ZiMissedSyncState.Synced) PixelGreenDark else Color.White,
                    enabled = state.selectedMissedCount > 0 &&
                        state.missedSyncState != ZiMissedSyncState.Syncing &&
                        state.missedSyncState != ZiMissedSyncState.Synced,
                    onClick = onSyncMissedChars,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("zi_game_sync_missed_button"),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PixelButton(
                text = "🚀 下一关 · ${nextStage.name}",
                background = PixelGreen,
                borderColor = PixelGreenLight,
                contentColor = PixelGreenDark,
                onClick = onNextStage,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("zi_game_next_stage_button"),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PixelButton(
                    text = "↻ 再玩本关",
                    background = Color(0xFF3B82F6),
                    borderColor = Color(0xFFBFDBFE),
                    contentColor = Color.White,
                    onClick = onReplay,
                    modifier = Modifier.weight(1f),
                )
                PixelButton(
                    text = "◀ 回到识字",
                    background = Color(0xFF1E2448),
                    borderColor = PixelBorderSlate,
                    contentColor = PixelTextLight,
                    onClick = onExit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("zi_game_exit_button"),
                )
            }
        }
    }
}

@Composable
private fun ResultStatCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0D1229))
            .border(2.dp, Color(0xFF334155))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = valueColor,
        )
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = PixelTextMuted,
        )
    }
}

@Composable
internal fun PixelPlanetIcon(
    stage: ZiGameStage,
    size: Dp,
    dimmed: Boolean = false,
) {
    val alpha = if (dimmed) 0.35f else 1f
    Canvas(modifier = Modifier.size(size)) {
        val grid = 9
        val cell = this.size.minDimension / grid
        val primary = stage.primaryColor.copy(alpha = alpha)
        val accent = stage.accentColor.copy(alpha = alpha)

        if (stage.kind == CelestialKind.DwarfOrAsteroid) {
            // Irregular rocky pixel asteroid silhouette
            val shape = listOf(
                "001110000",
                "011111100",
                "112211110",
                "112111111",
                "111111110",
                "011111110",
                "001111100",
                "000111000",
                "000000000",
            )
            shape.forEachIndexed { r, row ->
                row.forEachIndexed { c, ch ->
                    if (ch != '0') {
                        drawRect(
                            color = if (ch == '2') accent else primary,
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell + 0.5f, cell + 0.5f),
                        )
                    }
                }
            }
        } else {
            // Crisp pixel planet circle
            val shape = listOf(
                "000111000",
                "001221100",
                "012211110",
                "112111111",
                "111111111",
                "111111111",
                "011111110",
                "001111100",
                "000111000",
            )
            shape.forEachIndexed { r, row ->
                row.forEachIndexed { c, ch ->
                    if (ch != '0') {
                        drawRect(
                            color = if (ch == '2') accent else primary,
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell + 0.5f, cell + 0.5f),
                        )
                    }
                }
            }
            if (stage.hasRing) {
                drawRect(
                    color = accent,
                    topLeft = Offset(0f, 4 * cell),
                    size = Size(this.size.width, cell),
                )
            }
        }
    }
}

@Composable
private fun PixelButton(
    text: String,
    background: Color,
    borderColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val bg = if (enabled) background else background.copy(alpha = 0.45f)
    val bd = if (enabled) borderColor else borderColor.copy(alpha = 0.45f)
    Box(
        modifier = modifier
            .heightIn(min = if (compact) 32.dp else 46.dp)
            .background(bg, RectangleShape)
            .border(if (compact) 2.dp else 3.dp, bd, RectangleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 6.dp else 10.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (compact) 11.sp else 14.sp,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
        )
    }
}

/** Pixel-accented entry button shown on the main 识字 dashboard. */
@Composable
internal fun ZiTestGameEntryButton(
    readableCount: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("zi_test_game_button"),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1B4B),
        border = BorderStroke(2.dp, Color(0xFF6366F1)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFF59E0B))
                    .border(2.dp, PixelGoldLight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "字",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color(0xFF1E1B4B),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "汉字掉落测试",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.White,
                )
                val subtitle = if (readableCount != null && readableCount > 0) {
                    "太阳系闯关 · 语音烟花 · 20字一关"
                } else {
                    "离线语音读字 · 像素烟花游戏"
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFFC7D2FE),
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PixelGreen)
                    .border(2.dp, PixelGreenLight, RoundedCornerShape(4.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "测试 ▶",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = PixelGreenDark,
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Preview(widthDp = 320, heightDp = 640, locale = "zh")
@Composable
private fun ZiGameLobbyPreview() {
    ZiGameScreen(
        state = ZiGameUiState(
            phase = ZiGamePhase.Lobby,
            selectedStage = ZiGameLevels.stageForNumber(3),
            highestUnlockedStage = 10,
            stageStars = mapOf(1 to 3, 2 to 2, 3 to 3),
            bestScore = 1850,
            readablePoolSize = 128,
            isLoadingPool = false,
            hasMicPermission = true,
            modelState = ZiVoiceModelState.Ready,
        ),
    )
}

@Preview(widthDp = 320, heightDp = 640, locale = "zh")
@Composable
private fun ZiGamePlayingPreview() {
    ZiGameScreen(
        state = ZiGameUiState(
            phase = ZiGamePhase.Playing,
            selectedStage = ZiGameLevels.stageForNumber(6),
            totalCharsInStage = 20,
            completedCount = 4,
            hitCount = 4,
            score = 460,
            combo = 4,
            isListening = true,
            micLevel = 0.55f,
            currentTile = ZiFallingTile(zi = "木", pinyin = "mù", xFraction = 0.5f, yFraction = 0.42f),
            feedbackBanner = "正在听你读字... 可以读单字或组词哦！",
        ),
    )
}

@Preview(widthDp = 320, heightDp = 640, locale = "zh")
@Composable
private fun ZiGameResultPreview() {
    ZiGameScreen(
        state = ZiGameUiState(
            phase = ZiGamePhase.Result,
            selectedStage = ZiGameLevels.stageForNumber(7),
            highestUnlockedStage = 8,
            newlyUnlockedStage = ZiGameLevels.stageForNumber(8),
            totalCharsInStage = 20,
            completedCount = 20,
            hitCount = 18,
            score = 1920,
            maxCombo = 9,
            missedChars = listOf(
                ZiMissedChar("森", "sēn", selectedForReview = true),
                ZiMissedChar("桥", "qiáo", selectedForReview = true),
            ),
        ),
    )
}
