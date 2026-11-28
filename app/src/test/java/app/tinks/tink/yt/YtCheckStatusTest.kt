package app.tinks.tink.yt

import androidx.work.WorkInfo
import androidx.work.workDataOf
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class YtCheckStatusTest {
    private fun work(state: WorkInfo.State, manual: Boolean = true, progress: String = "", time: Long = 1) = WorkInfo(
        id = UUID.randomUUID(), state = state,
        tags = setOf("playlist:p", "requested:$time") + if (manual) setOf("manual") else emptySet(),
        progress = workDataOf("message" to progress))

    @Test fun queuedManualCheckExplainsWaitingAndRunningShowsProgress() {
        assertTrue(subscriptionCheckStates(listOf(work(WorkInfo.State.ENQUEUED)))["p"]!!.message.contains("waiting"))
        val running = subscriptionCheckStates(listOf(work(WorkInfo.State.RUNNING, progress = "Loading playlist…")))["p"]!!
        assertTrue(running.busy)
        assertEquals("Loading playlist…", running.message)
    }
    @Test fun futureScheduledCheckDoesNotBlockManualButtonAndFailureAllowsRetry() {
        assertTrue(subscriptionCheckStates(listOf(work(WorkInfo.State.ENQUEUED, manual = false))).isEmpty())
        val failed = subscriptionCheckStates(listOf(work(WorkInfo.State.FAILED)))["p"]!!
        assertFalse(failed.busy)
        assertTrue(failed.message.contains("failed"))
        assertTrue(subscriptionCheckStates(listOf(work(WorkInfo.State.FAILED), work(WorkInfo.State.SUCCEEDED, time = 2))).isEmpty())
    }
}
