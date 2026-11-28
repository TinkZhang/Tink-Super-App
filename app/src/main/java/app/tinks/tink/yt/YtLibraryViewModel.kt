package app.tinks.tink.yt

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class YtLibraryState(
    val downloads: List<YtDownload> = emptyList(),
    val subscriptions: List<YtSubscription> = emptyList(),
    val entries: List<YtPlaylistEntry> = emptyList(),
    val error: String? = null,
    val dates: Map<String, Long> = emptyMap(),
    val checks: Map<String, YtCheckState> = emptyMap(),
)

@Suppress("UNUSED_PARAMETER") // Old date caches are retained in storage, but playlist position is authoritative.
fun libraryEpisodes(downloads: List<YtDownload>, entries: List<YtPlaylistEntry>, group: String, dates: Map<String, Long> = emptyMap(), kind: YtDownloadKind = YtDownloadKind.AUDIO): List<YtDownload> {
    val ready = downloads.filter { it.kind == kind && it.status == YtDownloadStatus.READY }
    return when (group) {
        "all" -> ready
        "individual" -> ready.filter { it.standalone }
        else -> {
            if (group.startsWith("label:")) return ready.filter { item ->
                episodeLabels(item).any { labelKey(it) == group.removePrefix("label:") }
            }.sortedWith(compareBy<YtDownload> { it.createdAt }.thenBy { it.id })
            val order = entries.filter { it.playlistId == group && !it.dismissed && it.kind == kind }.associate { it.videoId to it.position }
            ready.filter { it.videoId in order }.sortedByDescending { order[it.videoId] }
        }
    }
}

@HiltViewModel
class YtLibraryViewModel @Inject constructor(
    private val db: YtDatabase,
    private val subscriptions: YtSubscriptions,
    private val downloads: YtDownloadRepository,
    private val files: YtMediaFiles,
    private val deletion: YtLibraryDeletion,
    val player: YtPlayer,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    val state = combine(db.downloads().observeAll(), subscriptions.subscriptions, subscriptions.entries, error, db.subscriptions().observeDates()) {
        items, groups, entries, message, dates -> YtLibraryState(items, groups, entries, message, dates.associate { it.videoId to it.uploadDate })
    }.combine(subscriptions.checks) { state, checks -> state.copy(checks = checks) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YtLibraryState())

    init { player.connect(); action { subscriptions.restoreSchedules() }; refresh() }
    override fun onCleared() { player.disconnect() }

    fun setLabels(item: YtDownload, input: String) = action {
        val labels = parseEpisodeLabels(input).joinToString("\n")
        check(db.downloads().setLabels(item.id, labels) == 1) { "This individual episode is no longer available." }
    }

    fun refresh() = action { downloads.reconcile(); downloads.scheduleBackgroundQueue() }
    fun addPlaylist(url: String, regex: String, cron: String, latestCount: Int, autoDelete: Boolean, speed: Float, format: YtSubscriptionFormat, downloadAllOnce: Boolean) = action { subscriptions.add(url, regex, cron, latestCount, autoDelete, speed, format, downloadAllOnce) }
    fun editPlaylist(id: String, regex: String, enabled: Boolean, cron: String, latestCount: Int, autoDelete: Boolean, speed: Float, format: YtSubscriptionFormat, downloadAllOnce: Boolean) = action { subscriptions.edit(id, regex, enabled, cron, latestCount, autoDelete, speed, format, downloadAllOnce) }
    fun checkNow(id: String) = action { subscriptions.checkNow(id) }
    fun removePlaylist(id: String, deleteDownloads: Boolean) = action {
        val items = subscriptions.remove(id, deleteDownloads)
        if (deleteDownloads) {
            withContext(Dispatchers.Main) { items.forEach { player.remove(it.id) } }
            deletion.delete(items.map { it.id })
        }
    }
    fun markPlayed(item: YtDownload) {
        player.remove(item.id)
        action { db.downloads().playback(item.id, 0, !item.played) }
    }
    fun delete(item: YtDownload) = deleteSelected(listOf(item))

    fun deleteSelected(items: List<YtDownload>) {
        items.distinctBy { it.id }.forEach { player.remove(it.id) }
        action { deletion.delete(items.map { it.id }) }
    }

    fun playGroup(group: String) {
        val queue = libraryEpisodes(state.value.downloads, state.value.entries, group, state.value.dates)
        val first = queue.firstOrNull() ?: return
        play(first, group, restartQueue = true)
    }

    fun play(item: YtDownload, group: String, restartQueue: Boolean = false) {
        viewModelScope.launch {
            try {
                val queue = libraryEpisodes(state.value.downloads, state.value.entries, group, state.value.dates)
                val valid = withContext(Dispatchers.IO) { queue.mapNotNull { track ->
                    files.completedFile(track)?.let { track to Uri.fromFile(it) }
                } }
                // Subscription-only tracks retain their listening settings in the combined library.
                // Shared tracks use the first active membership; an explicit group always wins.
                val snapshot = state.value
                val trackSubscriptions = if (group == "all") valid.mapNotNull { (track, _) ->
                    if (track.standalone) null else snapshot.entries.firstOrNull {
                        it.videoId == track.videoId && !it.dismissed && it.kind == track.kind
                    }?.let { entry -> snapshot.subscriptions.find { it.playlistId == entry.playlistId } }
                        ?.let { track.id to it }
                }.toMap() else emptyMap()
                player.play(valid, item.id, group, snapshot.subscriptions.find { it.playlistId == group }?.playbackSpeed,
                    restartQueue, trackSubscriptions)
                refresh()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error.value = "Could not open this local audio file. Try downloading it again." }
        }
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            error.value = null
            try { withContext(Dispatchers.IO) { block() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error.value = failure.message ?: "Could not update your library." }
        }
    }
}
