package app.tinks.tink.yt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YtUiState(
    val url: String = "",
    val kind: YtDownloadKind = YtDownloadKind.AUDIO,
    val downloads: List<YtDownload> = emptyList(),
    val submitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class YtViewModel @Inject constructor(
    private val repository: YtDownloads,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val submitting = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    val state = combine(
        savedState.getStateFlow("yt_url", ""),
        savedState.getStateFlow("yt_kind", YtDownloadKind.AUDIO.name),
        repository.downloads, submitting, error,
    ) { url, kind, downloads, submitting, error ->
        YtUiState(url, YtDownloadKind.valueOf(kind), downloads, submitting, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YtUiState())

    fun changeUrl(value: String) { savedState["yt_url"] = value; error.value = null }
    fun changeKind(value: YtDownloadKind) { savedState["yt_kind"] = value.name }
    fun showError(value: String) { error.value = value }

    fun download() {
        if (submitting.value) return
        val url = savedState.get<String>("yt_url").orEmpty()
        val kind = YtDownloadKind.valueOf(savedState.get<String>("yt_kind") ?: YtDownloadKind.AUDIO.name)
        if (YouTubeVideoUrl.parse(url) == null) {
            error.value = "Enter a valid YouTube video link."
            return
        }
        action { repository.download(url, kind) }
    }

    fun retry(id: Int) = action { repository.retry(id) }
    fun cancel(id: Int) = action { repository.cancel(id) }
    fun refresh() = action { repository.reconcile() }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            submitting.value = true
            error.value = null
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error.value = failure.message ?: "Could not update downloads. Try again." }
            finally { submitting.value = false }
        }
    }
}
