package app.tinks.tink.yt

import androidx.lifecycle.SavedStateHandle
import app.tinks.tink.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YtViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun rejectsInvalidLinksAndRestoresInputAndKind() = runTest {
        val repository = FakeYtDownloads()
        val saved = SavedStateHandle(mapOf("yt_url" to "https://vimeo.com/123", "yt_kind" to "VIDEO"))
        val vm = YtViewModel(repository, saved)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertEquals(YtDownloadKind.VIDEO, vm.state.value.kind)
        vm.download()
        assertTrue(repository.submitted.isEmpty())
        assertEquals("Enter a valid YouTube video link.", vm.state.value.error)
        vm.changeUrl(YtDownloadRepositoryTest.URL)
        vm.download()
        assertEquals(listOf(YtDownloadKind.VIDEO), repository.submitted)
        val recreated = YtViewModel(repository, saved)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { recreated.state.collect {} }
        assertEquals(YtDownloadRepositoryTest.URL, recreated.state.value.url)
        assertEquals(YtDownloadKind.VIDEO, recreated.state.value.kind)
    }

    @Test fun observesLocalDownloadsAndReportsActionFailures() = runTest {
        val repository = FakeYtDownloads()
        val vm = YtViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        repository.downloads.value = listOf(YtDownload(videoId = "LbCHAQ7HKu4", kind = YtDownloadKind.AUDIO, attempt = "one"))
        assertEquals(1, vm.state.value.downloads.size)
        vm.retry(42)
        assertEquals("Retry failed", vm.state.value.error)
        assertFalse(vm.state.value.submitting)
    }
}

private class FakeYtDownloads : YtDownloads {
    override val downloads = MutableStateFlow<List<YtDownload>>(emptyList())
    val submitted = mutableListOf<YtDownloadKind>()
    override suspend fun download(url: String, kind: YtDownloadKind) { submitted += kind }
    override suspend fun retry(id: Int) { error("Retry failed") }
    override suspend fun cancel(id: Int) {}
    override suspend fun reconcile() {}
}
