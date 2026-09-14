package app.tinks.tink.random

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

internal class RandomWindowSession(private val window: Window) {
    private val controller = WindowCompat.getInsetsController(window, window.decorView)
    private var entered = false
    private var previousKeepAwake = false
    private var previousBehavior = controller.systemBarsBehavior
    private var visibleBars = WindowInsetsCompat.Type.systemBars()

    fun enter() {
        if (entered) return
        entered = true
        previousKeepAwake = window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
        previousBehavior = controller.systemBarsBehavior
        val insets = ViewCompat.getRootWindowInsets(window.decorView)
        visibleBars = listOf(WindowInsetsCompat.Type.statusBars(), WindowInsetsCompat.Type.navigationBars())
            .filter { insets?.isVisible(it) != false }.fold(0) { value, type -> value or type }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun leave() {
        if (!entered) return
        entered = false
        if (!previousKeepAwake) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        controller.systemBarsBehavior = previousBehavior
        controller.show(visibleBars)
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable
fun RandomPlayRoute(viewModel: RandomViewModel, audio: RandomAudio?, active: Boolean) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val play by viewModel.play.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val window = LocalContext.current.activity()?.window
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    // A completed result is only spoken once during this visit, never on resume or recreation.
    var spokenRound by remember { mutableIntStateOf(play.completedRound) }
    DisposableEffect(window, lifecycle, active) {
        val session = window?.let(::RandomWindowSession)
        fun sync() {
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (active && foreground) session?.enter() else {
                session?.leave(); viewModel.cancelRoll(); audio?.stop()
            }
        }
        val observer = LifecycleEventObserver { _, _ -> sync() }
        lifecycle.addObserver(observer); sync()
        onDispose { lifecycle.removeObserver(observer); session?.leave(); viewModel.cancelRoll(); audio?.stop() }
    }
    LaunchedEffect(play.rolling, play.round, active, foreground) {
        if (active && foreground && play.rolling) audio?.start(settings.type, settings.sound) else audio?.stop()
    }
    LaunchedEffect(play.completedRound, active, foreground) {
        if (play.completedRound > spokenRound) {
            spokenRound = play.completedRound
            if (active && foreground) audio?.speak(play.result.chinese(settings), settings.speech)
        }
    }
    RandomPlayScreen(settings, play, onRoll = { if (active && foreground) viewModel.roll(ValueAnimator.areAnimatorsEnabled()) })
}

@Composable
fun RandomPlayScreen(settings: RandomSettings, state: RandomPlayState, onRoll: () -> Unit = {}) {
    val progress = remember { Animatable(0f) }
    val rotation = remember { Animatable(state.rotation) }
    LaunchedEffect(state.round, state.rolling) {
        if (state.rolling) {
            progress.snapTo(0f)
            launch { progress.animateTo(1f, tween(state.durationMillis.toInt(), easing = LinearEasing)) }
            rotation.animateTo(state.targetRotation, tween(state.durationMillis.toInt(), easing = CubicBezierEasing(.14f, .65f, .12f, 1f)))
        } else {
            progress.snapTo(0f); rotation.snapTo(state.rotation)
        }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("random_play"), contentAlignment = Alignment.Center) {
        RandomBackdrop(settings.type, Modifier.matchParentSize())
        // The system owns the edge-back gesture; only the central region handles play taps.
        BoxWithConstraints(Modifier.widthIn(max = 520.dp).fillMaxWidth(.84f).fillMaxHeight(.72f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !state.rolling, role = Role.Button, onClickLabel = settings.type.action, onClick = onRoll)
            .testTag("random_play_area").semantics(mergeDescendants = true) {
                contentDescription = settings.type.action + if (!state.rolling && state.completedRound > 0) "，${state.result.chinese(settings)}" else ""
                stateDescription = if (state.rolling) "正在${settings.type.action}" else if (state.completedRound > 0) "已完成" else "准备就绪"
                liveRegion = LiveRegionMode.Polite
            }, contentAlignment = Alignment.Center) {
            RandomObjects(settings, state.result, progress.value, state.rolling, rotation.value, Modifier.width(minOf(maxWidth, maxHeight)).padding(12.dp).clearAndSetSemantics {})
        }
    }
}
