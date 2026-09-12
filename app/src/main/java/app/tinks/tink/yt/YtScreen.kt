package app.tinks.tink.yt

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun YtScreen(viewModel: YtViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.download()
    }
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    YtScreen(
        state = state,
        onUrlChange = viewModel::changeUrl,
        onKindChange = viewModel::changeKind,
        onDownload = {
            // Transfer jobs still work if notifications are declined.
            if (YouTubeVideoUrl.parse(state.url) != null &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.download()
            }
        },
        onRetry = viewModel::retry,
        onCancel = viewModel::cancel,
        onOpen = { item ->
            scope.launch {
                val file = withContext(Dispatchers.IO) { YtMediaFiles(context).completedFile(item) }
                if (file == null) {
                    viewModel.refresh()
                    viewModel.showError("The downloaded file is missing. Tap Retry to download it again.")
                } else {
                    try {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, item.kind.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    } catch (_: ActivityNotFoundException) {
                        viewModel.showError("Install an audio or video player to open this file.")
                    }
                }
            }
        },
    )
}

@Composable
fun YtScreen(
    state: YtUiState,
    onUrlChange: (String) -> Unit = {},
    onKindChange: (YtDownloadKind) -> Unit = {},
    onDownload: () -> Unit = {},
    onRetry: (Int) -> Unit = {},
    onCancel: (Int) -> Unit = {},
    onOpen: (YtDownload) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("yt_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Save for offline", style = MaterialTheme.typography.headlineSmall)
                Text("Download audio or video from a YouTube link.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = state.url,
                    onValueChange = onUrlChange,
                    label = { Text("YouTube link") },
                    placeholder = { Text("https://youtu.be/…") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("yt_url_input"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    YtDownloadKind.entries.forEach { kind ->
                        FilterChip(
                            selected = state.kind == kind,
                            onClick = { onKindChange(kind) },
                            label = { Text(kind.label) },
                            modifier = Modifier.testTag("yt_kind_${kind.name.lowercase()}"),
                        )
                    }
                }
                Text(
                    if (state.kind == YtDownloadKind.AUDIO) "Audio only · M4A" else "Video with audio · MP4 · up to 720p",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onDownload, enabled = state.url.isNotBlank() && !state.submitting,
                    modifier = Modifier.fillMaxWidth().testTag("yt_download_button")) {
                    Text(if (state.submitting) "Please wait…" else "Download ${state.kind.label.lowercase()}")
                }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("yt_error"))
                }
            }
        }
        item { Text("Your downloads", style = MaterialTheme.typography.titleLarge) }
        if (state.downloads.isEmpty()) {
            item {
                Text("No downloads yet. Saved files will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("yt_empty"))
            }
        }
        items(state.downloads, key = { it.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth().testTag("yt_download_${item.videoId}_${item.kind.name.lowercase()}")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 3,
                        overflow = TextOverflow.Ellipsis)
                    Text(item.kind.label, style = MaterialTheme.typography.labelMedium)
                    Text(downloadStatusText(item), modifier = Modifier.testTag("yt_status_${item.id}"))
                    if (item.status.isActive) {
                        if (item.status == YtDownloadStatus.DOWNLOADING && item.progress > 0) {
                            LinearProgressIndicator(progress = { item.progress / 100f }, modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    item.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    when {
                        item.status.isActive -> TextButton(onClick = { onCancel(item.id) },
                            modifier = Modifier.testTag("yt_cancel_${item.id}")) { Text("Cancel") }
                        item.status == YtDownloadStatus.READY -> TextButton(onClick = { onOpen(item) },
                            modifier = Modifier.testTag("yt_open_${item.id}")) { Text("Open") }
                        else -> TextButton(onClick = { onRetry(item.id) },
                            modifier = Modifier.testTag("yt_retry_${item.id}")) { Text("Retry") }
                    }
                }
            }
        }
    }
}

fun downloadStatusText(item: YtDownload): String = when (item.status) {
    YtDownloadStatus.QUEUED -> "Queued · waiting for network or Android"
    YtDownloadStatus.DOWNLOADING -> "Downloading · ${item.progress}%"
    YtDownloadStatus.PROCESSING -> "Preparing your file…"
    YtDownloadStatus.READY -> "Downloaded · " + String.format(Locale.ROOT, "%.1f MB", item.sizeBytes / 1_000_000.0)
    YtDownloadStatus.FAILED -> "Download failed"
    YtDownloadStatus.CANCELLED -> "Cancelled"
}
