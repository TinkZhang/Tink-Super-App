package app.tinks.tink.weight

import app.tinks.tink.network.ApiResult
import app.tinks.tink.testing.MainDispatcherRule
import app.tinks.tink.weight.data.Weight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WeightViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsWeightsAndUsesLatestAsDraft() = runTest {
        val latest = Weight(id = 2, weight = 141.0, createdTime = 2000L)
        val repository = FakeWeightRepository(initialWeights = mutableListOf(latest))

        val viewModel = WeightViewModel(repository)

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(141.0, uiState.weightControlCardUiState.newWeight)
        assertEquals(listOf(latest), uiState.allWeights)
        assertEquals(1, repository.getWeightsCalls)
    }

    @Test
    fun adjustWeight_marksDraftChanged() = runTest {
        val repository = FakeWeightRepository(
            initialWeights = mutableListOf(Weight(id = 1, weight = 141.0, createdTime = 1000L))
        )
        val viewModel = WeightViewModel(repository)

        viewModel.onEvent(WeightEvent.AdjustNewWeight(0.5f))

        val uiState = viewModel.uiState.value
        assertTrue(uiState.weightControlCardUiState.showConfirm)
        assertEquals(141.5, uiState.weightControlCardUiState.newWeight ?: 0.0, 0.0001)
    }

    @Test
    fun changeSelectedTrendIndex_updatesUiState() = runTest {
        val repository = FakeWeightRepository()
        val viewModel = WeightViewModel(repository)

        viewModel.onEvent(WeightEvent.ChangeSelectedTrendIndex(1))

        assertEquals(1, viewModel.uiState.value.trendChartCardUiState.selectedIndex)
    }

    @Test
    fun addWeight_postsDraftAndRefreshesWeights() = runTest {
        val repository = FakeWeightRepository(
            initialWeights = mutableListOf(Weight(id = 1, weight = 141.0, createdTime = 1000L))
        )
        val viewModel = WeightViewModel(repository)

        viewModel.onEvent(WeightEvent.AdjustNewWeight(-0.4f))
        viewModel.onEvent(WeightEvent.AddWeight)

        assertEquals(140.6, repository.addedWeights.single(), 0.0001)
        assertFalse(viewModel.uiState.value.weightControlCardUiState.showConfirm)
        assertEquals(2, repository.getWeightsCalls)
    }

    @Test
    fun deleteWeight_deletesAndRefreshesWeights() = runTest {
        val repository = FakeWeightRepository(
            initialWeights = mutableListOf(
                Weight(id = 1, weight = 141.0, createdTime = 1000L),
                Weight(id = 2, weight = 140.5, createdTime = 2000L),
            )
        )
        val viewModel = WeightViewModel(repository)

        viewModel.onEvent(WeightEvent.DeleteWeight(1))

        assertEquals(listOf(1), repository.deletedIds)
        assertEquals(listOf(2), viewModel.uiState.value.allWeights.map { it.id })
    }

    private class FakeWeightRepository(
        initialWeights: MutableList<Weight> = mutableListOf(),
    ) : WeightRepository(NoopWeightApi) {
        private val weights = initialWeights
        val addedWeights = mutableListOf<Double>()
        val deletedIds = mutableListOf<Int>()
        var getWeightsCalls = 0

        override fun getWeights(): Flow<ApiResult<List<Weight>>> {
            getWeightsCalls += 1
            return flowOf(ApiResult.Loading, ApiResult.Success(weights.sortedByDescending { it.createdTime }))
        }

        override fun addWeight(weight: Double): Flow<ApiResult<Weight>> {
            addedWeights.add(weight)
            val added = Weight(
                id = (weights.maxOfOrNull { it.id } ?: 0) + 1,
                weight = weight,
                createdTime = (weights.maxOfOrNull { it.createdTime } ?: 0L) + 1000L,
            )
            weights.add(added)
            return flowOf(ApiResult.Loading, ApiResult.Success(added))
        }

        override fun deleteWeight(id: Int): Flow<ApiResult<Unit>> {
            deletedIds.add(id)
            weights.removeAll { it.id == id }
            return flowOf(ApiResult.Loading, ApiResult.Success(Unit))
        }
    }

    private object NoopWeightApi : WeightApi {
        override suspend fun getWeights(): List<WeightDto> = emptyList()

        override suspend fun createWeight(payload: WeightCreateRequest): WeightDto =
            error("not used")

        override suspend fun deleteWeight(weightId: Int) = Unit
    }
}
