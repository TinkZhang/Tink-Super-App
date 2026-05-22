package app.tinks.tink.haircut

import app.tinks.tink.network.ApiResult
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HaircutRepositoryTest {

    @Test
    fun getHaircuts_emitsLoadingThenSuccess_sortedByDateDescending() = runTest {
        val api = FakeHaircutApi(
            haircuts = mutableListOf(
                HaircutDto(1, LocalDate(2026, 4, 20), "Old Shop", 30),
                HaircutDto(2, LocalDate(2026, 5, 20), "New Shop", 40),
            )
        )
        val repository = HaircutRepository(api)

        val emissions = repository.getHaircuts().take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        val success = emissions[1] as ApiResult.Success
        assertEquals(listOf(2, 1), success.data.map { it.id })
    }

    @Test
    fun addHaircut_postsCreatePayload() = runTest {
        val api = FakeHaircutApi()
        val repository = HaircutRepository(api)
        val date = LocalDate(2026, 5, 22)

        val emissions = repository.addHaircut(price = 45, shopName = "Tink Cuts", date = date)
            .take(2)
            .toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        val success = emissions[1] as ApiResult.Success
        assertEquals("Tink Cuts", api.createdPayloads.single().shopName)
        assertEquals(45, api.createdPayloads.single().price)
        assertEquals(date, api.createdPayloads.single().createdAt)
        assertEquals("Tink Cuts", success.data.shopName)
    }

    @Test
    fun deleteHaircut_callsApiWithId() = runTest {
        val api = FakeHaircutApi()
        val repository = HaircutRepository(api)

        val emissions = repository.deleteHaircut(12).take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        assertTrue(emissions[1] is ApiResult.Success)
        assertEquals(listOf(12), api.deletedIds)
    }

    @Test
    fun getHaircuts_emitsError_whenApiThrows() = runTest {
        val repository = HaircutRepository(FakeHaircutApi(error = IllegalStateException("boom")))

        val emissions = repository.getHaircuts().take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        assertTrue(emissions[1] is ApiResult.Error)
    }

    private class FakeHaircutApi(
        private val haircuts: MutableList<HaircutDto> = mutableListOf(),
        private val error: Throwable? = null,
    ) : HaircutApi {
        val createdPayloads = mutableListOf<HaircutCreateRequest>()
        val deletedIds = mutableListOf<Int>()

        override suspend fun getHaircuts(): List<HaircutDto> {
            error?.let { throw it }
            return haircuts
        }

        override suspend fun createHaircut(payload: HaircutCreateRequest): HaircutDto {
            error?.let { throw it }
            createdPayloads.add(payload)
            return HaircutDto(
                id = 99,
                createdAt = payload.createdAt ?: LocalDate(2026, 5, 22),
                shopName = payload.shopName,
                price = payload.price,
            )
        }

        override suspend fun deleteHaircut(haircutId: Int) {
            error?.let { throw it }
            deletedIds.add(haircutId)
        }
    }
}
