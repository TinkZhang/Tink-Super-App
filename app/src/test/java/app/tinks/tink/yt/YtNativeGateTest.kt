package app.tinks.tink.yt

import android.app.Application
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
class YtNativeGateTest {
    @Test fun cancelledWaiterDoesNotReleaseTheOwnersLock() = runBlocking {
        withTimeout(3000) {
            val gate = YtNativeGate()
            val release = CompletableDeferred<Unit>()
            val holder = async(start = CoroutineStart.UNDISPATCHED) { gate.run(1, "a", "download") { release.await() } }
            assertTrue(gate.snapshot().contains("owner=download"))
            val waiter = async(start = CoroutineStart.UNDISPATCHED) { gate.run(0, "p", "playlist") { fail("Cancelled waiter must not enter") } }
            waiter.cancelAndJoin()
            assertTrue(gate.snapshot().contains("owner=download"))
            release.complete(Unit); holder.await()
            gate.run(2, "b", "next") { assertTrue(gate.snapshot().contains("owner=next")) }
            assertEquals("owner=none", gate.snapshot())
        }
    }
    @Test fun failedOperationReleasesTheLock() = runBlocking {
        val gate = YtNativeGate()
        assertTrue(runCatching { gate.run(1, "a", "failed") { error("failure") } }.isFailure)
        assertEquals("owner=none", gate.snapshot())
        gate.run(2, "b", "retry") { assertTrue(gate.snapshot().contains("owner=retry")) }
    }
}
