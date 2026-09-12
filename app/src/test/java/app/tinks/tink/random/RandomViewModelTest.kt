package app.tinks.tink.random

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RandomViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = object : RandomSettingsStore {
        override val settings = MutableStateFlow(RandomSettings())
        override fun save(settings: RandomSettings) { this.settings.value = settings }
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun repeatedTaps_doNotStartAnotherRound() = runTest(dispatcher) {
        val model = RandomViewModel(store)
        model.roll(); model.roll(); runCurrent()
        assertTrue(model.play.value.rolling)
        assertEquals(1, model.play.value.round)
        advanceUntilIdle()
        assertFalse(model.play.value.rolling)
        assertEquals(1, model.play.value.completedRound)
    }
    @Test fun returningDuringRoll_cancelsPendingResult() = runTest(dispatcher) {
        val model = RandomViewModel(store)
        val initial = model.play.value.result
        model.roll(); runCurrent(); advanceTimeBy(500); model.cancelRoll(); advanceUntilIdle()
        assertEquals(initial, model.play.value.result)
        assertEquals(0, model.play.value.completedRound)
        assertFalse(model.play.value.rolling)
        assertNull(model.play.value.pending)
    }
    @Test fun preferencesRemainAfterPlay_andReducedMotionCompletesImmediately() = runTest(dispatcher) {
        val model = RandomViewModel(store)
        model.update(RandomSettings(type = RandomType.Dice, diceCount = 6, sound = false))
        model.preparePlay(); model.roll(animate = false); runCurrent()
        assertEquals(6, model.play.value.result.dice.size)
        assertFalse(model.play.value.rolling)
        assertFalse(model.settings.value.sound)
        assertEquals(0L, model.play.value.durationMillis)
    }
}
