package app.tinks.tink.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RandomPlayState(
    val result: RandomResult = RandomResult(),
    val pending: RandomResult? = null,
    val rolling: Boolean = false,
    val round: Int = 0,
    val completedRound: Int = 0,
    val rotation: Float = 0f,
    val targetRotation: Float = 0f,
    val durationMillis: Long = 0,
)

@HiltViewModel
class RandomViewModel internal constructor(private val store: RandomSettingsStore) : ViewModel() {
    @Inject constructor(repository: RandomRepository) : this(repository as RandomSettingsStore)
    val settings = store.settings
    private val mutablePlay = MutableStateFlow(RandomPlayState(result = RandomResult(dice = List(store.settings.value.diceCount) { (it + 2) % 6 + 1 })))
    val play = mutablePlay.asStateFlow()
    private var rollJob: Job? = null

    fun update(settings: RandomSettings) {
        store.save(settings)
        preparePlay()
    }

    fun preparePlay() {
        cancelRoll()
        mutablePlay.value = RandomPlayState(result = RandomResult(dice = List(settings.value.diceCount) { (it + 2) % 6 + 1 }))
    }

    fun roll(animate: Boolean = true) {
        if (play.value.rolling) return
        val config = settings.value
        val result = drawRandom(config)
        val previous = play.value
        val rotation = if (config.type == RandomType.Wheel) nextWheelRotation(previous.rotation, config.displaySections(), result.wheelIndex) else 0f
        val duration = if (animate) config.type.durationMillis else 0L
        mutablePlay.value = previous.copy(rolling = true, round = previous.round + 1, pending = result, targetRotation = rotation, durationMillis = duration)
        rollJob = viewModelScope.launch {
            delay(duration)
            mutablePlay.value = play.value.copy(rolling = false, result = result, pending = null, rotation = rotation, completedRound = previous.round + 1)
        }
    }

    fun cancelRoll() {
        rollJob?.cancel(); rollJob = null
        mutablePlay.value = play.value.copy(rolling = false, pending = null, targetRotation = play.value.rotation)
    }
}
