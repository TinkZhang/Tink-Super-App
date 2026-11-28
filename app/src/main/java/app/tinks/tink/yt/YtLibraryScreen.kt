package app.tinks.tink.yt

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import java.text.DateFormat
import java.util.Date

@Composable
fun YtLibraryRoute(downloadModel: YtViewModel, library: YtLibraryViewModel = hiltViewModel(), sharedUrl: String? = null, shareRequestId: String? = null, onShareConsumed: () -> Unit = {}) {
    val state by library.state.collectAsStateWithLifecycle()
    val transfer by downloadModel.state.collectAsStateWithLifecycle()
    val playback by library.player.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(library) { library.refresh(); onPauseOrDispose { } }
    YtLibraryContent(state, playback, transfer, sharedUrl = sharedUrl, shareRequestId = shareRequestId, onShareConsumed = onShareConsumed,
        onAdd = { url, kind, regex, playlist, cron, latest, autoDelete, speed, format, downloadAllOnce ->
            if (playlist) library.addPlaylist(url, regex, cron, latest, autoDelete, speed, format, downloadAllOnce)
            else { downloadModel.changeUrl(url); downloadModel.changeKind(kind); downloadModel.download() }
        },
        onPlay = { item, group -> library.play(item, group) }, onPlayGroup = library::playGroup, onToggle = library.player::toggle, onNext = library.player::next,
        onSeek = library.player::seek, onSpeed = library.player::speed,
        onDelete = library::delete, onDeleteSelected = library::deleteSelected, onPlayed = library::markPlayed, onLabels = library::setLabels,
        onRetry = downloadModel::retry, onCancel = downloadModel::cancel,
        onCheck = library::checkNow, onEdit = library::editPlaylist, onUnsubscribe = library::removePlaylist,
        onSubscriptionVideo = { item, group, restart ->
            library.player.pause()
            context.startActivity(Intent(context, YtVideoActivity::class.java).putExtra("downloadId", item.id).putExtra("group", group).putExtra("restart", restart))
        },
        onVideo = { item ->
            library.player.pause()
            context.startActivity(Intent(context, YtVideoActivity::class.java).putExtra("downloadId", item.id))
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YtLibraryContent(
    state: YtLibraryState,
    playback: YtPlaybackState = YtPlaybackState(),
    transfer: YtUiState = YtUiState(),
    onAdd: (String, YtDownloadKind, String, Boolean, String, Int, Boolean, Float, YtSubscriptionFormat, Boolean) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onPlay: (YtDownload, String) -> Unit = { _, _ -> },
    onPlayGroup: (String) -> Unit = {},
    onToggle: () -> Unit = {}, onNext: () -> Unit = {}, onSeek: (Long) -> Unit = {}, onSpeed: (Float) -> Unit = {},
    onDelete: (YtDownload) -> Unit = {}, onPlayed: (YtDownload) -> Unit = {},
    onRetry: (Int) -> Unit = {}, onCancel: (Int) -> Unit = {}, onVideo: (YtDownload) -> Unit = {},
    onCheck: (String) -> Unit = {}, onEdit: (String, String, Boolean, String, Int, Boolean, Float, YtSubscriptionFormat, Boolean) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onUnsubscribe: (String, Boolean) -> Unit = { _, _ -> },
    onDeleteSelected: (List<YtDownload>) -> Unit = {},
    onLabels: (YtDownload, String) -> Unit = { _, _ -> },
    onSubscriptionVideo: (YtDownload, String, Boolean) -> Unit = { _, _, _ -> },
    sharedUrl: String? = null, shareRequestId: String? = null, onShareConsumed: () -> Unit = {},
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var dialogUrl by rememberSaveable { mutableStateOf("") }
    var dialogKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(shareRequestId) {
        if (shareRequestId != null && sharedUrl != null) { dialogUrl = sharedUrl; dialogKey = shareRequestId; adding = true }
    }
    var deleting by remember { mutableStateOf<YtDownload?>(null) }
    var editing by remember { mutableStateOf<YtSubscription?>(null) }
    var labelling by remember { mutableStateOf<YtDownload?>(null) }
    var selectedLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var collapsedPlaylists by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(arrayListOf<Int>()) }
    var deletingSelected by remember { mutableStateOf(false) }
    var removeDownloads by rememberSaveable { mutableStateOf(true) }
    val selectedItems = state.downloads.filter { it.id in selectedIds && it.status == YtDownloadStatus.READY }
    val toggleSelection: (YtDownload) -> Unit = { item ->
        selectedIds = ArrayList(if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id)
    }
    var removing by remember { mutableStateOf<YtSubscription?>(null) }
    Column(Modifier.fillMaxSize().testTag("yt_library")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your listening library", style = MaterialTheme.typography.headlineSmall)
                Text("${state.downloads.count { it.status == YtDownloadStatus.READY }} downloads saved offline", style = MaterialTheme.typography.bodySmall)
            }
            if (tab == 0) YtIconAction(if (selecting) Icons.Default.Close else Icons.Default.Checklist,
                if (selecting) "Exit selection" else "Select episodes", { selecting = !selecting; selectedIds = arrayListOf() }, tag = "yt_select_items")
            IconButton(onClick = { dialogUrl = ""; dialogKey = null; adding = true }, modifier = Modifier.testTag("yt_add_link")) { Icon(Icons.Default.Add, "Add link") }
        }
        PrimaryTabRow(selectedTabIndex = tab) {
            listOf("Library", "Playlists", "Downloads").forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index; selecting = false; selectedIds = arrayListOf() }, text = { Text(title) }, icon = { Icon(listOf(Icons.Default.LibraryMusic, Icons.Default.Subscriptions, Icons.Default.Download)[index], null) }, modifier = Modifier.testTag("yt_tab_$index"))
            }
        }
        if (tab == 0 && selecting) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${selectedItems.size} selected", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            YtIconAction(Icons.Default.DeleteOutline, "Delete selected episodes", { deletingSelected = true },
                enabled = selectedItems.isNotEmpty(), tag = "yt_delete_selected")
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val message = state.error ?: transfer.error ?: playback.error
            if (message != null) item { Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("yt_library_error")) }
            if (tab == 0) {
                if (state.downloads.none { it.status == YtDownloadStatus.READY }) item {
                    EmptyLibrary("Ready when you are", "Add a YouTube audio link or follow a playlist. Downloaded episodes will appear here.")
                }
                state.subscriptions.forEach { subscription ->
                    val audios = libraryEpisodes(state.downloads, state.entries, subscription.playlistId, state.dates)
                    val videos = libraryEpisodes(state.downloads, state.entries, subscription.playlistId, state.dates, YtDownloadKind.VIDEO)
                    val episodes = audios + videos
                    if (episodes.isNotEmpty()) {
                        item(key = "group-${subscription.playlistId}") {
                            Column(Modifier.fillMaxWidth().testTag("yt_group_header_${subscription.playlistId}"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(subscription.title, style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.fillMaxWidth().testTag("yt_group_title_${subscription.playlistId}"))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    YtCover(subscription.artworkUrl ?: episodes.firstOrNull()?.let(::episodeArtwork), Modifier.size(56.dp))
                                    Text("${episodes.size} items", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                    val collapsed = subscription.playlistId in collapsedPlaylists
                                    IconButton(onClick = {
                                        collapsedPlaylists = ArrayList(if (collapsed) collapsedPlaylists - subscription.playlistId else collapsedPlaylists + subscription.playlistId)
                                    }, modifier = Modifier.testTag("yt_toggle_group_${subscription.playlistId}").semantics { stateDescription = if (collapsed) "Collapsed" else "Expanded" }) {
                                        Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                            if (collapsed) "Expand ${subscription.title}" else "Collapse ${subscription.title}")
                                    }
                                    IconButton(onClick = { if (audios.isNotEmpty()) onPlayGroup(subscription.playlistId) else videos.firstOrNull()?.let { onSubscriptionVideo(it, subscription.playlistId, true) } }, enabled = audios.isEmpty() || playback.connected,
                                        modifier = Modifier.testTag("yt_play_group_${subscription.playlistId}")) { Icon(Icons.Default.PlayCircle, "Play all, oldest first") }
                                }
                            }
                        }
                        if (subscription.playlistId !in collapsedPlaylists) items(episodes, key = { "${subscription.playlistId}-${it.id}" }) { item ->
                            EpisodeCard(item, playback, onPlay = { if (item.kind == YtDownloadKind.VIDEO) onSubscriptionVideo(item, subscription.playlistId, false) else onPlay(item, subscription.playlistId) }, onDelete = { deleting = item }, onPlayed = { onPlayed(item) }, onLabels = { labelling = item }, selecting = selecting, selected = item.id in selectedIds, onSelect = { toggleSelection(item) })
                        }
                    }
                }
                val labels = libraryLabels(state.downloads.filter { it.kind == YtDownloadKind.AUDIO })
                val activeLabel = selectedLabel?.takeIf { selected -> labels.any { labelKey(it) == selected } }
                val group = activeLabel?.let { "label:$it" } ?: "individual"
                val individuals = libraryEpisodes(state.downloads, state.entries, group)
                if (state.downloads.any { it.standalone && it.kind == YtDownloadKind.AUDIO }) item {
                    Text("Individual episodes", style = MaterialTheme.typography.titleLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(activeLabel == null, { selectedLabel = null }, label = { Text("All labels") })
                        labels.forEach { label -> FilterChip(activeLabel == labelKey(label), { selectedLabel = labelKey(label) },
                            label = { Text(label) }, modifier = Modifier.testTag("yt_label_${labelKey(label)}")) }
                    }
                    if (activeLabel != null) TextButton(onClick = { onPlayGroup(group) }, enabled = playback.connected && individuals.isNotEmpty(),
                        modifier = Modifier.testTag("yt_play_label")) { Text("Play label · earliest saved first") }
                    if (individuals.isEmpty()) Text("No downloaded audio in this group yet.")
                }
                items(individuals, key = { "individual-${it.id}" }) { item ->
                    EpisodeCard(item, playback, onPlay = { onPlay(item, group) }, onDelete = { deleting = item }, onPlayed = { onPlayed(item) }, onLabels = { labelling = item }, selecting = selecting, selected = item.id in selectedIds, onSelect = { toggleSelection(item) })
                }
            } else if (tab == 1) {
                if (state.subscriptions.isEmpty()) item { EmptyLibrary("Follow a playlist", "Tap + to add a playlist and an optional title filter.") }
                items(state.subscriptions, key = { it.playlistId }) { subscription ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val audios = libraryEpisodes(state.downloads, state.entries, subscription.playlistId, state.dates)
                    val videos = libraryEpisodes(state.downloads, state.entries, subscription.playlistId, state.dates, YtDownloadKind.VIDEO)
                    val episodes = audios + videos
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            YtCover(subscription.artworkUrl ?: episodes.firstOrNull()?.let(::episodeArtwork), Modifier.size(64.dp))
                            Text(subscription.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (subscription.format == YtSubscriptionFormat.AUDIO) Icons.Default.Headphones else Icons.Default.Videocam, subscription.format.label, Modifier.size(20.dp))
                            Icon(if (subscription.enabled) Icons.Default.Schedule else Icons.Default.PauseCircle, if (subscription.enabled) "Schedule" else "Schedule paused", Modifier.size(20.dp))
                            Text(if (subscription.downloadAllOnce) "One-time import" else subscription.cron, style = MaterialTheme.typography.labelMedium)
                            Icon(Icons.Default.Download, "Latest episodes", Modifier.size(20.dp)); Text(if (subscription.downloadAllOnce) "All" else "${subscription.latestCount}")
                            Icon(Icons.Default.Speed, "Playback speed", Modifier.size(20.dp)); Text("${subscription.playbackSpeed}×")
                            Icon(if (subscription.autoDelete) Icons.Default.AutoDelete else Icons.Default.Save, if (subscription.autoDelete) "Auto-delete on" else "Keep after playback", Modifier.size(20.dp))
                        }
                        var details by remember(subscription.playlistId) { mutableStateOf(false) }
                        if (details) {
                            Text("https://www.youtube.com/playlist?list=${subscription.playlistId}", style = MaterialTheme.typography.bodySmall)
                            if (subscription.titleRegex.isNotBlank()) Text("Filter: ${subscription.titleRegex}", style = MaterialTheme.typography.bodySmall)
                            if (subscription.nextCheck > 0 && subscription.enabled) Text("Next: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(subscription.nextCheck))}", style = MaterialTheme.typography.bodySmall)
                            Text(if (subscription.lastChecked == 0L) "Not checked yet" else "Checked: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(subscription.lastChecked))}", style = MaterialTheme.typography.bodySmall)
                        }
                        val checking = state.checks[subscription.playlistId]
                        checking?.let { Text(it.message, modifier = Modifier.testTag("yt_check_status_${subscription.playlistId}")); if (it.busy) LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        if (checking == null) subscription.checkMessage?.let { Text(it, modifier = Modifier.testTag("yt_check_result_${subscription.playlistId}")) }
                        subscription.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            YtIconAction(Icons.Default.Refresh, "Check now", { onCheck(subscription.playlistId) }, checking?.busy != true, "yt_check_now_${subscription.playlistId}")
                            if (audios.isNotEmpty()) YtIconAction(Icons.Default.PlayCircle, "Play audio, oldest first", { onPlayGroup(subscription.playlistId) }, playback.connected, "yt_play_group_${subscription.playlistId}")
                            if (videos.isNotEmpty()) YtIconAction(Icons.Default.VideoLibrary, "Play video, oldest first", { onSubscriptionVideo(videos.first(), subscription.playlistId, true) }, tag = "yt_play_videos_${subscription.playlistId}")
                            YtIconAction(Icons.Default.Edit, "Edit subscription", { editing = subscription })
                            YtIconAction(Icons.Default.Info, "Subscription details", { details = !details }, tag = "yt_subscription_details_${subscription.playlistId}")
                            YtIconAction(Icons.Default.DeleteOutline, "Remove subscription", { removeDownloads = true; removing = subscription })
                        }
                    } }
                }
            } else {
                if (state.downloads.isEmpty()) item { EmptyLibrary("No downloads yet", "Tap + to save audio or video for offline use.") }
                items(state.downloads, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth().testTag("yt_download_${item.videoId}_${item.kind.name.lowercase()}")) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${item.kind.label} · ${item.status.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelMedium)
                        if (item.status.isActive) LinearProgressIndicator(progress = { item.progress / 100f }, modifier = Modifier.fillMaxWidth())
                        item.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Row {
                            if (item.status.isActive) YtIconAction(Icons.Default.Close, "Cancel download", { onCancel(item.id) })
                            if (item.status in setOf(YtDownloadStatus.FAILED, YtDownloadStatus.CANCELLED)) YtIconAction(Icons.Default.Refresh, "Retry download", { onRetry(item.id) })
                            if (item.status == YtDownloadStatus.READY) YtIconAction(Icons.Default.PlayArrow, if (item.kind == YtDownloadKind.AUDIO) "Play audio" else "Play video", { if (item.kind == YtDownloadKind.AUDIO) onPlay(item, "all") else onVideo(item) }, tag = "yt_download_play_${item.id}")
                            if (item.standalone) YtIconAction(Icons.Default.Label, "Edit labels", { labelling = item }, tag = "yt_labels_${item.id}")
                            YtIconAction(Icons.Default.DeleteOutline, "Delete download", { deleting = item })
                        }
                    } }
                }
            }
        }
        if (playback.id != null) PodcastPlayer(playback, onToggle, onNext, onSeek, onSpeed)
    }
    if (adding) key(dialogKey) { AddListeningDialog(initialUrl = dialogUrl, onDismiss = { adding = false; onShareConsumed() }, onAdd = { url, kind, regex, playlist, cron, latest, autoDelete, speed, format, downloadAllOnce ->
        onAdd(url, kind, regex, playlist, cron, latest, autoDelete, speed, format, downloadAllOnce); adding = false; onShareConsumed()
    }) }
    deleting?.let { item -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete download?") },
        text = { Text("Remove “${item.title}” from this device and all playlists. Automatic checks will not download it again.") },
        confirmButton = { TextButton(onClick = { onDelete(item); deleting = null }, modifier = Modifier.testTag("yt_confirm_delete")) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } }) }
    if (deletingSelected) AlertDialog(onDismissRequest = { deletingSelected = false },
        title = { Text("Delete ${selectedItems.size} downloads?") },
        text = { Text("Remove the selected files from this device and all playlists. Automatic checks will not download them again.") },
        confirmButton = { TextButton(enabled = selectedItems.isNotEmpty(), onClick = {
            onDeleteSelected(selectedItems); selectedIds = arrayListOf(); selecting = false; deletingSelected = false
        }, modifier = Modifier.testTag("yt_confirm_delete_selected")) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deletingSelected = false }) { Text("Cancel") } })
    removing?.let { subscription -> AlertDialog(onDismissRequest = { removing = null }, title = { Text("Remove playlist?") },
        text = { Column {
            Row(Modifier.clickable { removeDownloads = !removeDownloads }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(removeDownloads, { removeDownloads = it }, modifier = Modifier.testTag("yt_remove_downloads"))
                Text("Delete downloads too")
            }
            Text(if (removeDownloads) "Stop checking this playlist and delete its downloads from this device and all groups."
                else "Stop checking this playlist. Downloads are kept as individual episodes.")
        } },
        confirmButton = { TextButton(onClick = { onUnsubscribe(subscription.playlistId, removeDownloads); removing = null },
            modifier = Modifier.testTag("yt_confirm_remove_playlist")) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } }) }
    labelling?.let { item -> EpisodeLabelsDialog(item, libraryLabels(state.downloads), { labelling = null }) { input ->
        onLabels(item, input); labelling = null
    } }
    editing?.let { item -> EditSubscriptionDialog(item, { editing = null }) { regex, enabled, cron, latest, autoDelete, speed, format, downloadAllOnce -> onEdit(item.playlistId, regex, enabled, cron, latest, autoDelete, speed, format, downloadAllOnce); editing = null } }
}

@Composable
private fun EmptyLibrary(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Default.Headphones, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EpisodeCard(item: YtDownload, playback: YtPlaybackState, onPlay: () -> Unit, onDelete: () -> Unit, onPlayed: () -> Unit, onLabels: () -> Unit = {}, selecting: Boolean = false, selected: Boolean = false, onSelect: () -> Unit = {}) {
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().testTag("yt_episode_${item.id}").clickable(
        enabled = selecting || item.kind == YtDownloadKind.VIDEO || playback.connected,
        onClickLabel = if (selecting) "Select ${item.title}" else "Play ${item.title}",
        onClick = if (selecting) onSelect else onPlay,
    )) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selecting) Checkbox(selected, { onSelect() }, modifier = Modifier.testTag("yt_select_${item.id}"))
            YtCover(remember(item.filePath, item.videoId) { episodeArtwork(item) },
                Modifier.width(96.dp).height(72.dp).testTag("yt_thumbnail_${item.id}"))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(if (item.played) "Played" else if (item.positionMs > 0) "Continue at ${audioTime(item.positionMs)}" else "Not played", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(downloadDateLabel(item), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("yt_download_date_${item.id}"))
                if (episodeLabels(item).isNotEmpty()) Text(episodeLabels(item).joinToString(" · "), style = MaterialTheme.typography.labelSmall)
                Text("${item.kind.label} · ${String.format(java.util.Locale.ROOT, "%.1f", item.sizeBytes / 1_000_000.0)} MB · Offline", style = MaterialTheme.typography.bodySmall)
            }
            if (!selecting) Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.testTag("yt_episode_menu_${item.id}")) { Icon(Icons.Default.MoreVert, "Episode options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(if (item.played) "Mark unplayed" else "Mark played") }, onClick = { onPlayed(); menu = false })
                    if (item.standalone) DropdownMenuItem(text = { Text("Edit labels") }, onClick = { onLabels(); menu = false }, modifier = Modifier.testTag("yt_edit_labels_${item.id}"))
                    DropdownMenuItem(text = { Text("Delete download") }, onClick = { onDelete(); menu = false })
                }
            }
        }
    }
}

fun audioTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
}

@Composable
private fun PodcastPlayer(state: YtPlaybackState, onToggle: () -> Unit, onNext: () -> Unit, onSeek: (Long) -> Unit, onSpeed: (Float) -> Unit) {
    var speeds by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth().testTag("yt_player")) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                YtCover(state.artwork, Modifier.size(40.dp))
                Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
            Slider(value = scrubbing ?: state.position.toFloat().coerceAtMost(state.duration.toFloat()),
                onValueChange = { scrubbing = it }, onValueChangeFinished = { scrubbing?.let { onSeek(it.toLong()) }; scrubbing = null },
                valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f), enabled = state.duration > 0)
            Text("${audioTime(state.position)} / ${audioTime(state.duration)}", style = MaterialTheme.typography.labelSmall)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                YtIconAction(Icons.Default.Replay10, "Back 10 seconds", { onSeek(state.position - 10_000) })
                FilledIconButton(onClick = onToggle, modifier = Modifier.testTag("yt_player_toggle")) { Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.playing) "Pause" else "Play") }
                IconButton(onClick = onNext, enabled = state.hasNext, modifier = Modifier.testTag("yt_player_next")) { Icon(Icons.Default.SkipNext, "Next episode") }
                Box {
                    TextButton(onClick = { speeds = true }, modifier = Modifier.testTag("yt_player_speed")) { Icon(Icons.Default.Speed, "Playback speed"); Text("${state.speed}×") }
                    DropdownMenu(expanded = speeds, onDismissRequest = { speeds = false }) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f).forEach { speed ->
                            DropdownMenuItem(text = { Text("$speed×") }, onClick = { onSpeed(speed); speeds = false })
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddListeningDialog(initialUrl: String = "", onDismiss: () -> Unit, onAdd: (String, YtDownloadKind, String, Boolean, String, Int, Boolean, Float, YtSubscriptionFormat, Boolean) -> Unit) {
    var url by rememberSaveable { mutableStateOf(initialUrl) }
    var regex by rememberSaveable { mutableStateOf("") }
    var cron by rememberSaveable { mutableStateOf("0 8 * * *") }
    var latest by rememberSaveable { mutableStateOf("1") }
    var autoDelete by rememberSaveable { mutableStateOf(true) }
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var format by rememberSaveable { mutableStateOf(YtSubscriptionFormat.AUDIO) }
    var downloadAllOnce by rememberSaveable { mutableStateOf(false) }
    var playlist by rememberSaveable { mutableStateOf(YouTubeVideoUrl.parse(initialUrl) == null && youtubePlaylistId(initialUrl) != null) }
    var kind by rememberSaveable { mutableStateOf(YtDownloadKind.AUDIO) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add to your library") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !playlist, onClick = { playlist = false }, label = { Text("Single link") })
                FilterChip(selected = playlist, onClick = { playlist = true }, label = { Text("Playlist") }, modifier = Modifier.testTag("yt_add_playlist"))
            }
            OutlinedTextField(url, { url = it }, label = { Text(if (playlist) "YouTube playlist link" else "YouTube link") }, placeholder = { Text(if (playlist) "https://www.youtube.com/playlist?list=…" else "https://youtu.be/…") }, modifier = Modifier.testTag("yt_add_url"))
            if (playlist) {
                SubscriptionFormatFields(format) { format = it }
                OutlinedTextField(regex, { regex = it }, label = { Text("Title regex (optional)") }, modifier = Modifier.testTag("yt_title_regex"))
                Text("Optional regex · e.g. (?i)history|science", style = MaterialTheme.typography.bodySmall)
                SubscriptionImportMode(downloadAllOnce) { downloadAllOnce = it }
                if (!downloadAllOnce) SubscriptionScheduleFields(cron, { cron = it }, latest, { latest = it })
                SubscriptionListeningFields(autoDelete, { autoDelete = it }, speed, { speed = it })
            } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { YtDownloadKind.entries.forEach { value ->
                FilterChip(kind == value, { kind = value }, label = { Text(value.label) }, modifier = Modifier.testTag("yt_kind_${value.name.lowercase()}"))
            } }
            if (!playlist) Text(if (kind == YtDownloadKind.AUDIO) "Audio only · M4A" else "Video with audio · MP4 · up to 720p", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("yt_add_error")) }
        }
    }, confirmButton = { TextButton(onClick = {
        error = runCatching {
            if (playlist) { require(youtubePlaylistId(url) != null) { "Enter a YouTube playlist link." }; titleFilter(regex); if (!downloadAllOnce) validateSubscriptionOptions(cron, latest.toIntOrNull() ?: 0) }
            else require(YouTubeVideoUrl.parse(url) != null) { "Enter a YouTube video link." }
        }.exceptionOrNull()?.message
        if (error == null) onAdd(url, kind, regex, playlist, cron, latest.toIntOrNull() ?: 1, autoDelete, speed, format, downloadAllOnce)
    }, enabled = url.isNotBlank(), modifier = Modifier.testTag("yt_add_confirm")) { Text(if (playlist) "Subscribe" else "Download") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun EditSubscriptionDialog(item: YtSubscription, onDismiss: () -> Unit, onSave: (String, Boolean, String, Int, Boolean, Float, YtSubscriptionFormat, Boolean) -> Unit) {
    var regex by rememberSaveable(item.playlistId) { mutableStateOf(item.titleRegex) }
    var cron by rememberSaveable(item.playlistId) { mutableStateOf(item.cron) }
    var latest by rememberSaveable(item.playlistId) { mutableStateOf(item.latestCount.toString()) }
    var autoDelete by rememberSaveable(item.playlistId) { mutableStateOf(item.autoDelete) }
    var speed by rememberSaveable(item.playlistId) { mutableFloatStateOf(item.playbackSpeed) }
    var format by rememberSaveable(item.playlistId) { mutableStateOf(item.format) }
    var downloadAllOnce by rememberSaveable(item.playlistId) { mutableStateOf(item.downloadAllOnce) }
    var enabled by rememberSaveable(item.playlistId) { mutableStateOf(item.enabled) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Playlist settings") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(regex, { regex = it }, label = { Text("Title regex") })
            SubscriptionFormatFields(format) { format = it }
            SubscriptionImportMode(downloadAllOnce) { downloadAllOnce = it }
            if (!downloadAllOnce) SubscriptionScheduleFields(cron, { cron = it }, latest, { latest = it })
            SubscriptionListeningFields(autoDelete, { autoDelete = it }, speed, { speed = it })
            Text("Changes apply to future checks.", style = MaterialTheme.typography.bodySmall)
            if (!downloadAllOnce) Row(verticalAlignment = Alignment.CenterVertically) { Switch(enabled, { enabled = it }); Text("Automatic checks", Modifier.padding(start = 8.dp)) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        error = runCatching { titleFilter(regex); if (!downloadAllOnce) validateSubscriptionOptions(cron, latest.toIntOrNull() ?: 0) }.exceptionOrNull()?.message
        if (error == null) onSave(regex, enabled, cron, latest.toIntOrNull() ?: 1, autoDelete, speed, format, downloadAllOnce)
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}


@Composable
private fun SubscriptionScheduleFields(cron: String, onCron: (String) -> Unit, latest: String, onLatest: (String) -> Unit) {
    OutlinedTextField(cron, onCron, label = { Text("Cron schedule") }, singleLine = true, modifier = Modifier.testTag("yt_cron"))
    Text("0 8 * * * = daily 08:00 · device time zone", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(latest, onLatest, label = { Text("Latest episodes (1–500)") }, singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        modifier = Modifier.testTag("yt_latest_count"))
    Text("First ${latest.toIntOrNull() ?: 1} playlist entries · title filter applies within this set", style = MaterialTheme.typography.bodySmall)
}


@Composable
internal fun SubscriptionListeningFields(autoDelete: Boolean, onAutoDelete: (Boolean) -> Unit, speed: Float, onSpeed: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Switch(autoDelete, onAutoDelete, modifier = Modifier.testTag("yt_auto_delete"))
        Text("Auto-delete after playback", modifier = Modifier.weight(1f))
    }
    Text("After completion. Skipped and individually saved files stay.", style = MaterialTheme.typography.bodySmall)
    var choosing by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { choosing = true }, modifier = Modifier.testTag("yt_subscription_speed")) { Text("Playback speed: $speed×") }
        DropdownMenu(choosing, { choosing = false }) {
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f).forEach { value ->
                DropdownMenuItem(text = { Text("$value×") }, onClick = { onSpeed(value); choosing = false })
            }
        }
    }
}

@Composable
private fun YtCover(model: String?, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Headphones, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!LocalInspectionMode.current && model != null) AsyncImage(model, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
    }
}

@Composable
internal fun EpisodeLabelsDialog(item: YtDownload, suggestions: List<String>, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var input by rememberSaveable(item.id) { mutableStateOf(episodeLabels(item).joinToString(", ")) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Episode labels") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(input, { input = it; error = null }, label = { Text("Labels, separated by commas") },
                placeholder = { Text("History, Interviews") }, modifier = Modifier.testTag("yt_labels_input"))
            Text("Use up to 10 labels. Remove a label from the field to ungroup this episode; the download is kept.", style = MaterialTheme.typography.bodySmall)
            if (suggestions.isNotEmpty()) {
                Text("Existing labels", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { label -> AssistChip(onClick = {
                        val selected = runCatching { parseEpisodeLabels(input) }.getOrDefault(emptyList())
                        if (selected.none { labelKey(it) == labelKey(label) }) input = if (input.isBlank()) label else "$input, $label"
                    }, label = { Text(label) }) }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        error = runCatching { parseEpisodeLabels(input) }.exceptionOrNull()?.message
        if (error == null) onSave(input)
    }, modifier = Modifier.testTag("yt_labels_save")) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
internal fun SubscriptionFormatFields(selected: YtSubscriptionFormat, onSelect: (YtSubscriptionFormat) -> Unit) {
    Text("Download format", style = MaterialTheme.typography.labelLarge)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        YtSubscriptionFormat.entries.forEach { format ->
            FilterChip(selected == format, { onSelect(format) }, label = { Text(format.label) }, leadingIcon = { Icon(if (format == YtSubscriptionFormat.AUDIO) Icons.Default.Headphones else Icons.Default.Videocam, null) }, modifier = Modifier.testTag("yt_subscription_format_${format.name.lowercase()}"))
        }
    }

}

@Composable
private fun YtIconAction(icon: ImageVector, description: String, onClick: () -> Unit, enabled: Boolean = true, tag: String = "") {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.testTag(tag)) { Icon(icon, description) }
}

@Composable
internal fun SubscriptionImportMode(all: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(all, onChange, modifier = Modifier.testTag("yt_download_all_once"))
        Icon(Icons.Default.PlaylistAddCheck, null, Modifier.padding(horizontal = 8.dp))
        Text("Download all once")
    }
    if (all) Text("All matching entries · no scheduled checks", style = MaterialTheme.typography.bodySmall)
}

internal fun downloadDateLabel(item: YtDownload): String {
    val timestamp = item.downloadedAt ?: item.createdAt
    val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
    return if (item.downloadedAt != null) "Downloaded $date" else "Added $date"
}
