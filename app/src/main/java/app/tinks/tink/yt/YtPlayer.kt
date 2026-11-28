package app.tinks.tink.yt

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class YtPlaybackState(
    val connected: Boolean = false,
    val id: Int? = null,
    val title: String = "",
    val playing: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val speed: Float = 1f,
    val hasNext: Boolean = false,
    val error: String? = null,
    val artwork: String? = null,
)

fun audioQueue(downloads: List<YtDownload>): List<YtDownload> = downloads.filter {
    it.kind == YtDownloadKind.AUDIO && it.status == YtDownloadStatus.READY
}

@Singleton
class YtPlayer @Inject constructor(@ApplicationContext private val context: Context) {
    private val mutable = MutableStateFlow(YtPlaybackState())
    val state = mutable.asStateFlow()
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var ticker: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) mutable.value = mutable.value.copy(error = null)
        }
        override fun onPlayerError(error: PlaybackException) {
            mutable.value = mutable.value.copy(error = "Cannot play this local file. Retry playback or download it again.")
            YtDownloadLog.failure(mutable.value.id ?: 0, null, "playback.failed", error)
        }
    }

    fun connect() {
        if (future != null) return
        val connection = MediaController.Builder(context,
            SessionToken(context, ComponentName(context, YtPlaybackService::class.java))).buildAsync()
        future = connection
        connection.addListener({
            if (future !== connection) return@addListener
            try {
                controller = connection.get().also { it.addListener(listener) }
                publish()
                ticker = scope.launch { while (isActive) { delay(500); publish() } }
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not connect to the audio player. Reopen YT to retry.")
                future = null
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun disconnect() {
        ticker?.cancel()
        controller?.removeListener(listener)
        future?.let(MediaController::releaseFuture)
        future = null
        controller = null
    }

    fun play(items: List<Pair<YtDownload, Uri>>, selected: Int, group: String, speed: Float? = null, restartQueue: Boolean = false, subscriptionsByTrack: Map<Int, YtSubscription> = emptyMap()) {
        val player = controller ?: return
        val index = items.indexOfFirst { it.first.id == selected }
        if (index < 0) { mutable.value = mutable.value.copy(error = "This local file is missing. Download it again."); return }
        mutable.value = mutable.value.copy(error = null)
        if (!restartQueue && player.currentMediaItem?.mediaId == selected.toString() && player.currentMediaItem?.mediaMetadata?.extras?.getString("group") == group) {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.prepare(); player.play(); return
        }
        val media = items.map { (item, uri) ->
            val subscription = subscriptionsByTrack[item.id]
            val trackGroup = subscription?.playlistId ?: group
            val trackSpeed = subscription?.playbackSpeed ?: speed
            MediaItem.Builder().setMediaId(item.id.toString()).setUri(uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(item.title)
                    .setArtworkUri(episodeArtwork(item)?.let(Uri::parse))
                    .setExtras(Bundle().apply { putString("group", trackGroup); trackSpeed?.let { putFloat("speed", it) } }).build()).build()
        }
        player.shuffleModeEnabled = false
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.setMediaItems(media, index, if (restartQueue) 0 else items[index].first.positionMs)
        player.prepare()
        player.play()
    }

    fun pause() { controller?.pause() }
    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else { if (it.playbackState == Player.STATE_ENDED) it.seekTo(0); it.prepare(); it.play() } } }
    fun next() { controller?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() } }
    fun seek(position: Long) { controller?.seekTo(position.coerceIn(0, mutable.value.duration.coerceAtLeast(0))) }
    fun speed(value: Float) { controller?.setPlaybackSpeed(value.coerceIn(0.5f, 3f)) }
    fun remove(id: Int) {
        controller?.let { player ->
            for (index in player.mediaItemCount - 1 downTo 0) {
                if (player.getMediaItemAt(index).mediaId == id.toString()) player.removeMediaItem(index)
            }
        }
    }

    private fun publish() {
        val p = controller ?: return
        mutable.value = YtPlaybackState(true, p.currentMediaItem?.mediaId?.toIntOrNull(),
            p.mediaMetadata.title?.toString().orEmpty(), p.isPlaying, p.currentPosition.coerceAtLeast(0),
            p.duration.coerceAtLeast(0), p.playbackParameters.speed, p.hasNextMediaItem(), mutable.value.error, p.mediaMetadata.artworkUri?.toString())
    }
}
