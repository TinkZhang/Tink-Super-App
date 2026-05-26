package app.tinks.tink.merriam

import app.tinks.tink.merriam.data.Root
import app.tinks.tink.merriam.data.Stat
import app.tinks.tink.merriam.data.Unit
import app.tinks.tink.merriam.db.MerriamDao
import app.tinks.tink.merriam.db.RootEntity
import app.tinks.tink.merriam.network.MerriamApi
import app.tinks.tink.merriam.network.RootPostDto
import app.tinks.tink.network.ApiResult
import app.tinks.tink.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MerriamViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsStatAndLocalUnits() = runTest {
        val repository = FakeMerriamRepository(
            initialUnits = listOf(sampleUnit()),
            stat = Stat(latest = 11, weekStats = listOf(1, 0, 2, 0, 0, 0, null)),
        )

        val viewModel = MerriamViewModel(repository)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(11, uiState.latest)
        assertEquals(listOf(sampleUnit()), uiState.units)
        assertEquals(7, uiState.weeklyRecords.records.size)
        assertEquals(1, repository.getStatCalls)
    }

    @Test
    fun completeRoot_postsMissingRootsAndOptimisticallyUpdatesLatest() = runTest {
        val repository = FakeMerriamRepository(
            initialUnits = listOf(sampleUnit()),
            stat = Stat(latest = 10, weekStats = emptyWeekStats()),
        )
        val viewModel = MerriamViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(MerriamEvent.CompleteRoot(12))
        advanceUntilIdle()

        assertEquals(
            listOf(
                RootPostDto(rootId = 11, root = "BENE"),
                RootPostDto(rootId = 12, root = "AM"),
            ),
            repository.postedRecords.single(),
        )
        assertEquals(12, viewModel.uiState.value.latest)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun completeRoot_ignoresAlreadyCompletedRoot() = runTest {
        val repository = FakeMerriamRepository(
            initialUnits = listOf(sampleUnit()),
            stat = Stat(latest = 12, weekStats = emptyWeekStats()),
        )
        val viewModel = MerriamViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(MerriamEvent.CompleteRoot(11))
        advanceUntilIdle()

        assertEquals(emptyList<List<RootPostDto>>(), repository.postedRecords)
        assertEquals(12, viewModel.uiState.value.latest)
    }

    @Test
    fun refresh_loadsStatAgain() = runTest {
        val repository = FakeMerriamRepository(
            initialUnits = listOf(sampleUnit()),
            stat = Stat(latest = 11, weekStats = emptyWeekStats()),
        )
        val viewModel = MerriamViewModel(repository)
        advanceUntilIdle()

        repository.stat = Stat(latest = 12, weekStats = listOf(1, 0, 0, 0, 0, 0, null))
        viewModel.onEvent(MerriamEvent.Refresh)
        advanceUntilIdle()

        assertEquals(2, repository.getStatCalls)
        assertEquals(12, viewModel.uiState.value.latest)
    }

    private fun sampleUnit(): Unit = Unit(
        id = 1,
        roots = listOf(
            Root(id = 11, unit = 1, text = "BENE", meaning = "Well"),
            Root(id = 12, unit = 1, text = "AM", meaning = "To love"),
        ),
    )

    private fun emptyWeekStats(): List<Int?> = listOf(0, 0, 0, 0, 0, 0, null)

    private class FakeMerriamRepository(
        initialUnits: List<Unit>,
        var stat: Stat,
    ) : MerriamRepository(NoopMerriamDao, NoopMerriamApi) {
        private val units = MutableStateFlow(initialUnits)
        val postedRecords = mutableListOf<List<RootPostDto>>()
        var getStatCalls = 0

        override fun getAllUnitsFlow(): Flow<List<Unit>> = units

        override fun addMerriamRecords(records: List<RootPostDto>): Flow<ApiResult<kotlin.Unit>> {
            postedRecords.add(records)
            return flowOf(ApiResult.Loading, ApiResult.Success(kotlin.Unit))
        }

        override fun getMerriamStat(): Flow<ApiResult<Stat>> {
            getStatCalls += 1
            return flowOf(ApiResult.Loading, ApiResult.Success(stat))
        }
    }

    private object NoopMerriamDao : MerriamDao {
        override fun getAllRootsFlow(): Flow<List<RootEntity>> = flowOf(emptyList())
    }

    private object NoopMerriamApi : MerriamApi {
        override suspend fun getStat() = error("not used")
        override suspend fun postMerriam(request: List<RootPostDto>) = error("not used")
    }
}
