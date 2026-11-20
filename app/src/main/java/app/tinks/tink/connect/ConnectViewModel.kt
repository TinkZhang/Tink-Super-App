package app.tinks.tink.connect

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ConnectUiState(
    val hub: ConnectHub = ConnectHub(),
    val draft: String = "",
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val uploadingName: String? = null,
    val progress: Float? = null,
    val retryUpload: Boolean = false,
    val message: String? = null,
)

internal fun ConnectUiState.withHub(value: ConnectHub): ConnectUiState =
    copy(hub = value, draft = if (draft == hub.text) value.text else draft, loaded = true)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val api: ConnectApi,
    private val transfers: ConnectTransfers,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(ConnectUiState())
    val state = _state.asStateFlow()
    private val pending = ArrayDeque<PendingConnectUpload>()
    private val resolver get() = context.contentResolver

    fun edit(text: String) = _state.update { it.copy(draft = text) }
    fun dismissMessage() = _state.update { it.copy(message = null) }
    fun refresh() = runOperation("Couldn’t load Connect") { reload() }
    fun share() = runOperation("Couldn’t share text") {
        val draft = _state.value.draft
        val result = api.share(ConnectText(draft))
        _state.update { it.copy(hub = it.hub.copy(text = result.text)) }
    }

    fun upload(uris: List<Uri>) = runOperation("Couldn’t upload files") {
        for (uri in uris) pending.add(stage(uri))
        sendPending()
    }

    fun retry() = runOperation("Couldn’t upload files") { sendPending() }

    fun discardUpload() {
        if (_state.value.busy) return
        viewModelScope.launch {
            val abandoned = pending.toList()
            pending.clear()
            _state.update { it.copy(retryUpload = false, uploadingName = null, progress = null) }
            abandoned.forEach { item ->
                item.source.delete()
                item.ticket?.let { ticket -> runCatching { api.delete(ticket.id) } }
            }
        }
    }

    fun download(file: ConnectFile, destination: Uri) = runOperation("Couldn’t download ${file.name}") {
        val url = api.download(file.id).url
        withContext(Dispatchers.IO) {
            val staged = File.createTempFile("connect-", ".download", context.cacheDir)
            try {
                transfers.download(url, staged, file.size)
                val stream = resolver.openOutputStream(destination, "wt") ?: throw IOException("Destination unavailable")
                staged.inputStream().use { input -> stream.use { output -> input.copyTo(output) } }
            } finally {
                staged.delete()
            }
        }
        _state.update { it.copy(message = "Downloaded ${file.name}") }
    }

    fun delete(file: ConnectFile) = runOperation("Couldn’t delete ${file.name}") {
        api.delete(file.id)
        reload()
    }

    fun clear() = runOperation("Couldn’t clear Connect") {
        api.clear()
        pending.forEach { it.source.delete() }
        pending.clear()
        _state.update { it.copy(hub = ConnectHub(), draft = "", uploadingName = null, progress = null) }
        reload()
    }

    private suspend fun reload() {
        val hub = api.hub()
        _state.update { it.withHub(hub) }
    }

    private suspend fun stage(uri: Uri): PendingConnectUpload = withContext(Dispatchers.IO) {
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }?.takeIf { it.isNotBlank() } ?: "File"
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val file = File.createTempFile("connect-", ".upload", context.cacheDir)
        try {
            val input = resolver.openInputStream(uri) ?: throw IOException("File unavailable")
            input.use { source -> file.outputStream().use { source.copyTo(it) } }
            PendingConnectUpload(file, ConnectUploadRequest(name, mime, file.length()))
        } catch (failure: Exception) {
            file.delete()
            throw failure
        }
    }

    private suspend fun sendPending() {
        while (pending.isNotEmpty()) {
            val item = pending.first()
            _state.update { it.copy(uploadingName = item.request.name, progress = null, retryUpload = false) }
            val ticket = item.ticket ?: api.begin(item.request).also { item.ticket = it }
            try {
                if (!item.started || !completeIfStored(ticket)) {
                    item.started = true
                    withContext(Dispatchers.IO) {
                        transfers.upload(item, ticket) { percent -> _state.update { it.copy(progress = percent) } }
                    }
                    api.complete(ticket.id)
                }
            } catch (failure: Exception) {
                val expired = when (failure) {
                    is ConnectTransferException -> failure.status in setOf(401, 403, 404, 410)
                    is retrofit2.HttpException -> failure.code() == 404
                    else -> false
                }
                if (expired) {
                    api.delete(ticket.id)
                    item.ticket = null
                    item.location = null
                    item.started = false
                }
                throw failure
            }
            pending.removeFirst()
            item.source.delete()
        }
        _state.update { it.copy(uploadingName = null, progress = null) }
        reload()
    }

    private suspend fun completeIfStored(ticket: ConnectUpload): Boolean {
        return try {
            api.complete(ticket.id)
            true
        } catch (failure: retrofit2.HttpException) {
            if (failure.code() != 409) throw failure
            false
        }
    }

    private fun runOperation(error: String, operation: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.update { it.copy(message = error, retryUpload = pending.isNotEmpty()) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    override fun onCleared() {
        pending.forEach { it.source.delete() }
    }
}
