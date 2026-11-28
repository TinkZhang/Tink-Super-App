package app.tinks.tink.zi.game

import app.tinks.tink.network.ApiResult
import app.tinks.tink.zi.Zi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ZiGameViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleZis(count: Int): List<Zi> {
        val pool = listOf(
            "木", "山", "水", "火", "月", "日", "森", "林", "花", "云",
            "雨", "风", "星", "鸟", "桥", "石", "田", "土", "竹", "马",
            "人", "口", "手", "天", "地",
        )
        return pool.take(count).map { Zi(it, LocalDate(2026, 1, 1), proficiency = 5) } +
            listOf(Zi("写", LocalDate(2026, 1, 1), proficiency = 2))
    }

    @Test
    fun loadsOnlyReadableCharactersAndCapsStageAtTwenty() = runTest(dispatcher) {
        val vm = ZiGameViewModel(
            characterSource = FakeCharacterSource(sampleZis(25)),
            modelProvider = FakeVoiceModelProvider(ready = true),
            speechEngine = FakeSpeechEngine(),
            progressRepository = FakeProgressRepository(),
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(25, state.readablePoolSize)
        assertEquals(20, state.totalCharsInStage)
        assertEquals("水星", state.selectedStage.name)
        assertEquals(ZiGameSpeed.Slow, state.speed)
    }

    @Test
    fun speechHitTriggersFireworkComboScoreAndAdvancesStage() = runTest(dispatcher) {
        val speech = FakeSpeechEngine()
        val vm = ZiGameViewModel(
            characterSource = FakeCharacterSource(sampleZis(2)),
            modelProvider = FakeVoiceModelProvider(ready = true),
            speechEngine = speech,
            progressRepository = FakeProgressRepository(),
        )
        advanceUntilIdle()
        vm.updateMicPermission(true)

        vm.startGame()
        assertEquals(ZiGamePhase.Playing, vm.uiState.value.phase)
        assertEquals(2, vm.uiState.value.totalCharsInStage)
        val firstTile = vm.uiState.value.currentTile
        assertNotNull(firstTile)

        // Say the character -> hit + firework particles + score
        vm.onSpeechRecognized(firstTile!!.zi)
        assertEquals(1, vm.uiState.value.hitCount)
        assertEquals(1, vm.uiState.value.combo)
        assertTrue(vm.uiState.value.score >= 100)
        assertTrue(vm.uiState.value.particles.isNotEmpty())
        assertNull(vm.uiState.value.currentTile)

        // Advance time to spawn second character
        repeat(16) { vm.tick(0.05f) }
        val secondTile = vm.uiState.value.currentTile
        assertNotNull(secondTile)

        // Say second character -> stage clears and unlocks Venus (stage 2)
        vm.onSpeechRecognized(secondTile!!.zi)
        repeat(16) { vm.tick(0.05f) }

        val result = vm.uiState.value
        assertEquals(ZiGamePhase.Result, result.phase)
        assertEquals(2, result.hitCount)
        assertEquals(100, result.accuracyPercent)
        assertEquals(3, result.earnedStars)
        assertEquals(2, result.highestUnlockedStage)
        assertEquals("金星", result.newlyUnlockedStage?.name)
        assertTrue(speech.stoppedCount >= 1)
    }

    @Test
    fun fallingToGroundRecordsMissedCharAndSyncsSelectedToReview() = runTest(dispatcher) {
        val source = FakeCharacterSource(sampleZis(2))
        val vm = ZiGameViewModel(
            characterSource = source,
            modelProvider = FakeVoiceModelProvider(ready = true),
            speechEngine = FakeSpeechEngine(),
            progressRepository = FakeProgressRepository(),
        )
        advanceUntilIdle()
        vm.updateMicPermission(true)
        vm.startGame()

        // Let both characters fall all the way to ground
        repeat(600) {
            if (vm.uiState.value.phase == ZiGamePhase.Playing) vm.tick(0.05f)
        }

        val result = vm.uiState.value
        assertEquals(ZiGamePhase.Result, result.phase)
        assertEquals(2, result.missedChars.size)
        assertEquals(2, result.selectedMissedCount)

        // Uncheck the first missed character, then sync only the remaining one
        val unselected = result.missedChars.first().zi
        val expectedSynced = result.missedChars.last().zi
        vm.toggleMissedSelection(unselected)
        assertEquals(1, vm.uiState.value.selectedMissedCount)

        vm.syncMissedToReview()
        advanceUntilIdle()

        assertEquals(ZiMissedSyncState.Synced, vm.uiState.value.missedSyncState)
        assertEquals(listOf(expectedSynced to ZiGameViewModel.REVIEW_PROFICIENCY_LEVEL), source.downgradedCalls)
    }

    private class FakeCharacterSource(
        private val items: List<Zi>,
    ) : ZiGameCharacterDataSource {
        val downgradedCalls = mutableListOf<Pair<String, Int>>()

        override fun getAllZis(): Flow<ApiResult<List<Zi>>> = flowOf(ApiResult.Success(items))

        override fun downgradeMissedToReview(zis: String, proficiency: Int): Flow<ApiResult<Unit>> {
            downgradedCalls += zis to proficiency
            return flowOf(ApiResult.Loading, ApiResult.Success(Unit))
        }
    }

    private class FakeVoiceModelProvider(private val ready: Boolean) : ZiVoiceModelProvider {
        override fun isModelReady(): Boolean = ready
        override fun modelFilesOrNull(): ZiVoiceModelFiles? =
            if (ready) ZiVoiceModelFiles("enc", "dec", "joi", "tok") else null

        override fun ensureModelReady(): Flow<ZiVoiceModelState> = flowOf(ZiVoiceModelState.Ready)
    }

    private class FakeSpeechEngine : ZiSpeechEngine {
        var stoppedCount = 0
        override fun start(
            modelFiles: ZiVoiceModelFiles,
            onPartialText: (String) -> Unit,
            onMicLevel: (Float) -> Unit,
            onError: (String) -> Unit,
        ) = Unit

        override fun resetUtterance() = Unit
        override fun stop() {
            stoppedCount += 1
        }
    }

    private class FakeProgressRepository : ZiGameProgressRepository {
        private var saved = ZiGameSavedProgress()
        override fun load(): ZiGameSavedProgress = saved
        override fun saveSpeed(speed: ZiGameSpeed) {
            saved = saved.copy(speed = speed)
        }

        override fun recordStageResult(stageNumber: Int, score: Int, stars: Int): ZiGameSavedProgress {
            val nextStars = saved.stageStars.toMutableMap().apply {
                this[stageNumber] = maxOf(this[stageNumber] ?: 0, stars)
            }
            saved = saved.copy(
                highestUnlockedStage = maxOf(saved.highestUnlockedStage, stageNumber + 1),
                bestScore = maxOf(saved.bestScore, score),
                stageStars = nextStars,
            )
            return saved
        }
    }
}
