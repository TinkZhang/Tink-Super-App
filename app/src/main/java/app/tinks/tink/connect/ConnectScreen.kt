package app.tinks.tink.connect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ConnectScreen(viewModel: ConnectViewModel, onOpenDrawer: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val upload = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        if (it.isNotEmpty()) viewModel.upload(it)
    }
    var downloadId by rememberSaveable { mutableStateOf<String?>(null) }
    val download = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { destination ->
        val file = state.hub.files.find { it.id == downloadId }
        downloadId = null
        if (destination != null && file != null) viewModel.download(file, destination)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    ConnectContent(
        state = state,
        onOpenDrawer = onOpenDrawer,
        onRefresh = viewModel::refresh,
        onUpload = { upload.launch(arrayOf("*/*")) },
        onRetry = viewModel::retry,
        onDiscard = viewModel::discardUpload,
        onDownload = {
            downloadId = it.id
            download.launch(it.name.substringAfterLast('/').substringAfterLast('\\').ifBlank { "File" })
        },
        onDelete = viewModel::delete,
        onClear = viewModel::clear,
        onEdit = viewModel::edit,
        onPaste = {
            clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)
                ?.let { viewModel.edit(it.toString()) }
        },
        onCopy = { clipboard.setPrimaryClip(ClipData.newPlainText("Connect", state.hub.text)) },
        onShare = viewModel::share,
        onDismissMessage = viewModel::dismissMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectContent(
    state: ConnectUiState,
    onOpenDrawer: () -> Unit,
    onRefresh: () -> Unit,
    onUpload: () -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    onDownload: (ConnectFile) -> Unit,
    onDelete: (ConnectFile) -> Unit,
    onClear: () -> Unit,
    onEdit: (String) -> Unit,
    onPaste: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    var confirmAll by rememberSaveable { mutableStateOf(false) }
    var confirmFileId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val files = state.hub.files
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            onDismissMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Connect") },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "Open navigation menu") } },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.busy) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface {
                OutlinedButton(
                    onClick = { confirmAll = true },
                    enabled = !state.busy && (files.isNotEmpty() || state.hub.text.isNotEmpty() || state.retryUpload),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)
                        .testTag("connect_delete_all"),
                ) {
                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete all content")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("connect_hub"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Files", style = MaterialTheme.typography.titleLarge)
                        if (state.loaded) {
                            Text(fileCount(files.size), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(
                        onClick = onUpload,
                        enabled = !state.busy && !state.retryUpload,
                        modifier = Modifier.testTag("connect_upload"),
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upload")
                    }
                }
            }
            if (state.uploadingName != null || state.retryUpload) {
                item {
                    Card(colors = connectCardColors()) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(state.uploadingName ?: "Upload interrupted", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (state.busy) {
                                if (state.progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                                else LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                            }
                            if (state.retryUpload && !state.busy) {
                                Row {
                                    TextButton(onClick = onRetry) { Text("Retry") }
                                    TextButton(onClick = onDiscard) { Text("Discard") }
                                }
                            }
                        }
                    }
                }
            }
            if (files.isEmpty()) {
                item {
                    Card(colors = connectCardColors(), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!state.loaded && state.busy) CircularProgressIndicator(Modifier.size(24.dp))
                            else Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                if (state.loaded) "No files yet" else if (state.busy) "Loading Connect…" else "Connect unavailable",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (state.loaded) Text("Files you upload appear here on both devices.", style = MaterialTheme.typography.bodySmall)
                            if (!state.loaded && !state.busy) TextButton(onClick = onRefresh) { Text("Retry") }
                        }
                    }
                }
            }
            items(files, key = { it.id }) { file ->
                ConnectFileRow(file, state.busy, { onDownload(file) }, { confirmFileId = file.id })
            }
            item {
                Card(colors = connectCardColors(), modifier = Modifier.padding(top = 6.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Shared text", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = state.draft,
                            onValueChange = onEdit,
                            placeholder = { Text("Paste or type text to share") },
                            modifier = Modifier.fillMaxWidth().testTag("connect_shared_text"),
                            minLines = 2,
                            maxLines = 5,
                        )
                        Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            TextButton(onClick = onPaste) { Text("Paste") }
                            TextButton(onClick = onCopy, enabled = state.hub.text.isNotEmpty()) { Text("Copy") }
                            FilledTonalButton(
                                onClick = onShare,
                                enabled = !state.busy && state.draft != state.hub.text,
                                modifier = Modifier.testTag("connect_share_text"),
                            ) { Text("Share") }
                        }
                    }
                }
            }
        }
    }

    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            icon = { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete all content?") },
            text = {
                Text("This permanently deletes all files and clears the shared text on both devices. Downloaded copies stay on your devices.")
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Cancel") } },
            confirmButton = {
                Button(
                    onClick = { confirmAll = false; onClear() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("connect_confirm_clear"),
                ) { Text("Delete all") }
            },
        )
    }

    files.find { it.id == confirmFileId }?.let { file ->
        AlertDialog(
            onDismissRequest = { confirmFileId = null },
            title = { Text("Delete file?") },
            text = { Text("${file.name} will be removed from Connect on both devices.") },
            dismissButton = { TextButton(onClick = { confirmFileId = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = { confirmFileId = null; onDelete(file) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}

@Composable
private fun ConnectFileRow(file: ConnectFile, busy: Boolean, onDownload: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Card(colors = connectCardColors()) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onDownload).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 10.dp)) {
                Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                val size = Formatter.formatFileSize(context, file.size)
                Text("$size · ${formatConnectDate(file.createdAt)}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDownload, enabled = !busy) { Icon(Icons.Default.Download, "Download ${file.name}") }
            Box {
                IconButton(onClick = { menu = true }, enabled = !busy) { Icon(Icons.Default.MoreVert, "More actions for ${file.name}") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Download") }, onClick = { menu = false; onDownload() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun connectCardColors() =
    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

private fun fileCount(count: Int) = if (count == 1) "1 file" else "$count files"

private fun formatConnectDate(value: String): String = runCatching {
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())
        .format(Instant.parse(value))
}.getOrDefault(value)
