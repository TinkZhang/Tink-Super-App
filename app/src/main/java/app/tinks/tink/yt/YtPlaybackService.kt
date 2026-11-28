package app.tinks.tink.yt

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.tinks.tink.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

@AndroidEntryPoint
class YtPlaybackService : MediaSessionService() {
    @Inject lateinit var dao: YtDownloadDao
    @Inject lateinit var subscriptions: YtSubscriptionDao
    @Inject lateinit var completion: YtPlaybackCompletion
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val completed = mutableSetOf<Int>()
    private var settings = emptyMap<String, YtSubscription>()
    private val preferences by lazy { getSharedPreferences("yt-playback", MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        // Persist in event order. Drain remaining writes when the service closes; an end event
        // must not be overwritten by a delayed position update or lost during service teardown.
        CoroutineScope(Dispatchers.IO).launch {
            for (write in writes) try { write() }
            catch (error: Exception) { YtDownloadLog.failure(0, null, "playback.persist.failed", error) }
        }
        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
            repeatMode = Player.REPEAT_MODE_OFF
            shuffleModeEnabled = false
        }
        fun applyGroupSpeed() {
            val item = player.currentMediaItem ?: return
            val group = item.mediaMetadata.extras?.getString("group") ?: "individual"
            val speed = settings[group]?.playbackSpeed ?: item.mediaMetadata.extras?.getFloat("speed", 0f)?.takeIf { it > 0 }
                ?: preferences.getFloat("speed-$group", 1f)
            if (player.playbackParameters.speed != speed) player.setPlaybackSpeed(speed.coerceIn(0.5f, 3f))
        }
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.toIntOrNull()?.let { completed.remove(it) }
                applyGroupSpeed()
            }
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                val group = player.currentMediaItem?.mediaMetadata?.extras?.getString("group") ?: "individual"
                val subscription = settings[group]
                if (subscription != null) {
                    if (subscription.playbackSpeed != playbackParameters.speed) writes.trySend { subscriptions.setPlaybackSpeed(group, playbackParameters.speed) }
                } else preferences.edit().putFloat("speed-$group", playbackParameters.speed).apply()
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                val old = oldPosition.mediaItem ?: return
                val id = old.mediaId.toIntOrNull() ?: return
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION && old.mediaId != newPosition.mediaItem?.mediaId) {
                    finish(old)
                } else {
                    val same = old.mediaId == newPosition.mediaItem?.mediaId
                    if (same && reason == Player.DISCONTINUITY_REASON_SEEK) completed.remove(id)
                    record(id, (if (same) newPosition.positionMs else oldPosition.positionMs).coerceAtLeast(0))
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { save(player) }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && player.duration > 0 && player.currentPosition >= player.duration) {
                    player.currentMediaItem?.let(::finish)
                }
            }
        })
        val activity = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, player).setSessionActivity(activity).build()
        scope.launch { subscriptions.observe().collect { values -> settings = values.associateBy { it.playlistId }; applyGroupSpeed() } }
        scope.launch { while (isActive) { delay(5_000); if (player.isPlaying) save(player) } }
    }

    private fun record(id: Int, position: Long) {
        if (id !in completed) writes.trySend { dao.playback(id, position, false) }
    }

    private fun save(player: Player) {
        val id = player.currentMediaItem?.mediaId?.toIntOrNull() ?: return
        record(id, player.currentPosition.coerceAtLeast(0))
    }

    private fun finish(item: MediaItem) {
        val id = item.mediaId.toIntOrNull() ?: return
        if (!completed.add(id)) return
        val group = item.mediaMetadata.extras?.getString("group") ?: "individual"
        writes.trySend {
            // The next item already plays automatically. Remove the completed item from
            // the queue after cleanup; no additional play/seek command is needed.
            val remove = completion.complete(id, group)
            if (remove) withContext(Dispatchers.Main.immediate) {
                session?.player?.let { player ->
                    for (index in player.mediaItemCount - 1 downTo 0) {
                        if (player.getMediaItemAt(index).mediaId == id.toString()) player.removeMediaItem(index)
                    }
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.let { media -> save(media.player); media.player.release(); media.release() }
        session = null
        scope.cancel()
        writes.close()
        super.onDestroy()
    }
}
