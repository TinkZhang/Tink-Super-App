package app.tinks.tink.yt

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.clickable
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import app.tinks.tink.ui.theme.TinkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** A private local-file destination. No URL is accepted from the launching intent. */
@AndroidEntryPoint
class YtVideoActivity : ComponentActivity() {
    @Inject lateinit var db: YtDatabase
    @Inject lateinit var files: YtMediaFiles
    @Inject lateinit var completion: YtPlaybackCompletion
    private var player: ExoPlayer? = null
    private var listener: Player.Listener? = null
    private var state by mutableStateOf(YtVideoState())
    private var fullscreen by mutableStateOf(false)
    private var downloadId = 0
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val completed = mutableSetOf<Int>()
    private var group = "individual"
    private var subscriptionGroup = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyFullscreen(savedInstanceState?.getBoolean("fullscreen") ?: false)
        downloadId = savedInstanceState?.getInt("downloadId") ?: intent.getIntExtra("downloadId", 0)
        group = intent.getStringExtra("group") ?: "individual"
        CoroutineScope(Dispatchers.IO).launch {
            for (write in writes) try { write() }
            catch (error: Exception) { YtDownloadLog.failure(downloadId, null, "video.save.failed", error) }
        }
        setContent { TinkTheme {
            BackHandler(fullscreen) { applyFullscreen(false) }
            YtVideoContent(state, fullscreen = fullscreen, onFullscreen = { applyFullscreen(!fullscreen) }, onBack = ::finish, onToggle = {
                player?.let { p ->
                    if (p.isPlaying) p.pause() else {
                        if (p.playbackState == Player.STATE_ENDED) p.seekTo(0)
                        p.prepare(); p.play()
                    }
                }
            }, onSeek = { player?.seekTo(it.coerceIn(0, state.duration)) }, onSpeed = { player?.setPlaybackSpeed(it) }) {
                val p = player
                if (p != null) BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val ratio = state.aspectRatio
                    val fit = if (maxWidth / maxHeight > ratio) Modifier.fillMaxHeight().aspectRatio(ratio)
                        else Modifier.fillMaxWidth().aspectRatio(ratio)
                    AndroidView(factory = { context -> SurfaceView(context).also { p.setVideoSurfaceView(it) } },
                        modifier = fit, onRelease = { p.clearVideoSurfaceView(it) })
                }
            }
        } }
        lifecycleScope.launch {
            val item = withContext(Dispatchers.IO) { db.downloads().get(downloadId) }
            val file = withContext(Dispatchers.IO) { item?.takeIf(::isLocalVideo)?.let(files::completedFile) }
            if (item == null || file == null) {
                state = state.copy(loading = false, error = "This downloaded video is missing. Return to Downloads and retry it.")
                return@launch
            }
            val subscription = withContext(Dispatchers.IO) {
                if (group == "individual" && !item.standalone) group = db.subscriptions().memberships(item.videoId, item.kind).firstOrNull()?.playlistId ?: group
                db.subscriptions().get(group)
            }
            subscriptionGroup = subscription != null
            val queue = withContext(Dispatchers.IO) {
                val candidates = if (subscription == null) listOf(item) else libraryEpisodes(db.downloads().all(), db.subscriptions().entries(group), group,
                    db.subscriptions().observeDates().first().associate { it.videoId to it.uploadDate }, YtDownloadKind.VIDEO)
                candidates.mapNotNull { track -> files.completedFile(track)?.let { track to it } }
            }
            val selectedIndex = queue.indexOfFirst { it.first.id == item.id }
            if (selectedIndex < 0) { state = state.copy(loading = false, error = "This video is no longer in the subscription."); return@launch }
            val p = ExoPlayer.Builder(this@YtVideoActivity).build()
            player = p
            state = state.copy(title = item.title)
            p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            p.setHandleAudioBecomingNoisy(true)
            val events = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) { publish(); persist() }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.mediaId?.toIntOrNull()?.let { downloadId = it; completed.remove(it) }
                    state = state.copy(title = mediaItem?.mediaMetadata?.title?.toString() ?: state.title, aspectRatio = 16f / 9f)
                }
                override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                    val old = oldPosition.mediaItem?.mediaId?.toIntOrNull() ?: return
                    if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) finishEpisode(old)
                    else {
                        val same = oldPosition.mediaItem?.mediaId == newPosition.mediaItem?.mediaId
                        if (same && reason == Player.DISCONTINUITY_REASON_SEEK) completed.remove(old)
                        record(old, if (same) newPosition.positionMs else oldPosition.positionMs)
                    }
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) p.currentMediaItem?.mediaId?.toIntOrNull()?.let(::finishEpisode)
                }
                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                    if (subscriptionGroup) writes.trySend { db.subscriptions().setPlaybackSpeed(group, playbackParameters.speed) }
                }
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) state = state.copy(
                        aspectRatio = (videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height).coerceIn(0.1f, 10f))
                }
                override fun onPlayerError(error: PlaybackException) {
                    state = state.copy(loading = false, error = "Cannot play this video. Try again, or download it again from Downloads.")
                    YtDownloadLog.failure(downloadId, null, "video.playback.failed", error)
                }
            }
            listener = events
            p.addListener(events)
            p.setMediaItems(queue.map { (track, local) -> MediaItem.Builder().setMediaId(track.id.toString()).setUri(Uri.fromFile(local))
                .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).build()).build() }, selectedIndex,
                savedInstanceState?.getLong("position") ?: if (intent.getBooleanExtra("restart", false) || item.played) 0 else item.positionMs.coerceAtLeast(0))
            p.setPlaybackSpeed(savedInstanceState?.getFloat("speed", 1f) ?: subscription?.playbackSpeed ?: 1f)
            p.prepare()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && (savedInstanceState?.getBoolean("playing", true) != false)) p.play()
            while (isActive) { delay(1_000); publish(); if (p.isPlaying) persist() }
        }
    }

    private fun publish() {
        player?.let { p ->
            state = state.copy(playing = p.isPlaying, position = p.currentPosition.coerceAtLeast(0), duration = p.duration.coerceAtLeast(0),
                speed = p.playbackParameters.speed, loading = p.playbackState == Player.STATE_BUFFERING,
                error = if (p.playbackState == Player.STATE_READY) null else state.error, available = p.currentMediaItem != null)
            if (p.isPlaying) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun record(id: Int, position: Long) {
        if (id !in completed) writes.trySend { db.downloads().playback(id, position.coerceAtLeast(0), false) }
    }

    private fun persist() {
        player?.let { p -> p.currentMediaItem?.mediaId?.toIntOrNull()?.let { record(it, p.currentPosition) } }
    }

    private fun finishEpisode(id: Int) {
        if (!completed.add(id)) return
        writes.trySend {
            if (completion.complete(id, group)) withContext(Dispatchers.Main.immediate) {
                player?.let { p -> for (index in p.mediaItemCount - 1 downTo 0) {
                    if (p.getMediaItemAt(index).mediaId == id.toString()) p.removeMediaItem(index)
                } }
            }
        }
    }

    private fun applyFullscreen(value: Boolean) {
        fullscreen = value
        val bars = WindowCompat.getInsetsController(window, window.decorView)
        bars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (value) bars.hide(WindowInsetsCompat.Type.systemBars()) else bars.show(WindowInsetsCompat.Type.systemBars())
        requestedOrientation = if (value) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("fullscreen", fullscreen)
        player?.let { p -> outState.putInt("downloadId", downloadId); outState.putLong("position", p.currentPosition); outState.putFloat("speed", p.playbackParameters.speed); outState.putBoolean("playing", p.playWhenReady) }
        super.onSaveInstanceState(outState)
    }
    override fun onStop() { player?.pause(); persist(); super.onStop() }
    override fun onDestroy() {
        persist()
        player?.let { p -> listener?.let(p::removeListener); p.release() }; player = null
        writes.close()
        super.onDestroy()
    }
}

fun isLocalVideo(item: YtDownload): Boolean = item.kind == YtDownloadKind.VIDEO && item.status == YtDownloadStatus.READY
fun videoProgress(position: Long, ended: Boolean): Pair<Long, Boolean> = (if (ended) 0 else position.coerceAtLeast(0)) to ended

data class YtVideoState(val title: String = "Downloaded video", val playing: Boolean = false,
    val position: Long = 0, val duration: Long = 0, val speed: Float = 1f,
    val loading: Boolean = true, val available: Boolean = false, val error: String? = null, val aspectRatio: Float = 16f / 9f)

@Composable
fun YtVideoContent(state: YtVideoState, onBack: () -> Unit = {}, onToggle: () -> Unit = {},
    onSeek: (Long) -> Unit = {}, onSpeed: (Float) -> Unit = {}, fullscreen: Boolean = false,
    onFullscreen: () -> Unit = {}, video: @Composable () -> Unit = {}) {
    var controls by remember(fullscreen) { mutableStateOf(true) }
    var choosing by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(fullscreen, controls, state.playing, choosing, scrubbing != null) {
        if (fullscreen && controls && state.playing && !choosing && scrubbing == null) { delay(3_000); controls = false }
    }
    Surface(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }.testTag("yt_video_player"),
        color = if (fullscreen) Color.Black else MaterialTheme.colorScheme.surface,
        contentColor = if (fullscreen) Color.White else MaterialTheme.colorScheme.onSurface) {
        Box(Modifier.fillMaxSize()) {
            if (fullscreen) Box(Modifier.fillMaxSize().clickable { controls = !controls }.testTag("yt_video_surface"), contentAlignment = Alignment.Center) {
                video()
                if (state.loading) CircularProgressIndicator()
            }
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                if (!fullscreen || controls) Row(Modifier.fillMaxWidth().background(if (fullscreen) Color.Black.copy(alpha = 0.65f) else Color.Transparent), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (fullscreen) onFullscreen() else onBack() }, modifier = Modifier.testTag("yt_video_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, if (fullscreen) "Exit fullscreen" else "Back to downloads") }
                    Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                }
                if (fullscreen) Spacer(Modifier.weight(1f)) else Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black), contentAlignment = Alignment.Center) {
                    video()
                    if (state.loading) CircularProgressIndicator()
                }
                if (!fullscreen || controls || state.error != null) Column(Modifier.background(if (fullscreen) Color.Black.copy(alpha = 0.65f) else Color.Transparent)) {
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp).testTag("yt_video_error")) }
                    Slider(value = scrubbing ?: state.position.toFloat().coerceIn(0f, state.duration.toFloat()),
                        onValueChange = { scrubbing = it }, onValueChangeFinished = { scrubbing?.let { onSeek(it.toLong()) }; scrubbing = null },
                        valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f), enabled = state.available && state.duration > 0,
                        modifier = Modifier.padding(horizontal = 16.dp).testTag("yt_video_seek"))
                    Text(audioTime(state.position) + " / " + audioTime(state.duration), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { onSeek((state.position - 10_000).coerceAtLeast(0)) }, enabled = state.available) { Icon(Icons.Default.Replay10, "Back 10 seconds") }
                        IconButton(onClick = onToggle, enabled = state.available, modifier = Modifier.testTag("yt_video_toggle")) { Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.playing) "Pause video" else "Play video") }
                        IconButton(onClick = { onSeek((state.position + 10_000).coerceAtMost(state.duration)) }, enabled = state.available) { Icon(Icons.Default.Forward10, "Forward 10 seconds") }
                        Box {
                            TextButton(onClick = { choosing = true }, enabled = state.available, modifier = Modifier.testTag("yt_video_speed")) { Text("${state.speed}×") }
                            DropdownMenu(choosing, { choosing = false }) {
                                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f).forEach { speed -> DropdownMenuItem(text = { Text("$speed×") }, onClick = { choosing = false; onSpeed(speed) }) }
                            }
                        }
                        IconButton(onClick = onFullscreen, modifier = Modifier.testTag("yt_video_fullscreen")) { Icon(if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, if (fullscreen) "Exit fullscreen" else "Fullscreen") }
                    }
                }
            }
        }
    }
}
