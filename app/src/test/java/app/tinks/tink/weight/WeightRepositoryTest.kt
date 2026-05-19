package app.tinks.tink.weight

import app.tinks.tink.network.ApiResult
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightRepositoryTest {

    @Test
    fun getWeights_emitsLoadingThenSuccess_withWeightsSortedDescending() = runTest {
        val api = FakeWeightApi(
            weights = mutableListOf(
                WeightDto(id = 1, createdAt = "2026-05-18T08:00:00Z", weight = 141.8),
                WeightDto(id = 2, createdAt = "2026-05-19T08:00:00Z", weight = 141.2),
            )
        )
        val repository = WeightRepository(api)

        val emissions = repository.getWeights().take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        val success = emissions[1] as ApiResult.Success
        assertEquals(listOf(2, 1), success.data.map { it.id })
    }

    @Test
    fun addWeight_postsCreatePayload() = runTest {
        val api = FakeWeightApi()
        val repository = WeightRepository(api)

        val emissions = repository.addWeight(140.6).take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        val success = emissions[1] as ApiResult.Success
        assertEquals(140.6, api.createdPayloads.single().weight, 0.0)
        assertEquals(140.6, success.data.weight, 0.0)
    }

    @Test
    fun deleteWeight_callsApiWithWeightId() = runTest {
        val api = FakeWeightApi()
        val repository = WeightRepository(api)

        val emissions = repository.deleteWeight(42).take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        assertTrue(emissions[1] is ApiResult.Success)
        assertEquals(listOf(42), api.deletedIds)
    }

    @Test
    fun getWeights_emitsError_whenApiThrows() = runTest {
        val repository = WeightRepository(FakeWeightApi(error = IllegalStateException("boom")))

        val emissions = repository.getWeights().take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        assertTrue(emissions[1] is ApiResult.Error)
    }

    private class FakeWeightApi(
        private val weights: MutableList<WeightDto> = mutableListOf(),
        private val error: Throwable? = null,
    ) : WeightApi {
        val createdPayloads = mutableListOf<WeightCreateRequest>()
        val deletedIds = mutableListOf<Int>()

        override suspend fun getWeights(): List<WeightDto> {
            error?.let { throw it }
            return weights
        }

        override suspend fun createWeight(payload: WeightCreateRequest): WeightDto {
            error?.let { throw it }
            createdPayloads.add(payload)
            return WeightDto(id = 99, createdAt = "2026-05-19T08:00:00Z", weight = payload.weight)
        }

        override suspend fun deleteWeight(weightId: Int) {
            error?.let { throw it }
            deletedIds.add(weightId)
        }
    }
}
