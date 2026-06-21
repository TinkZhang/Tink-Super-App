package app.tinks.tink.merriam

import app.tinks.tink.merriam.data.StatDto
import app.tinks.tink.merriam.db.MerriamDao
import app.tinks.tink.merriam.db.RootEntity
import app.tinks.tink.merriam.network.MerriamApi
import app.tinks.tink.merriam.network.RootPostDto
import app.tinks.tink.network.ApiResult
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MerriamRepositoryTest {

    @Test
    fun getMerriamStat_emitsLoadingThenSuccess() = runBlocking {
        val api = FakeMerriamApi(
            statDto = StatDto(
                latest = 9,
                weekStats = listOf(1, 2, null)
            )
        )
        val repository = MerriamRepository(
            dao = FakeMerriamDao(),
            api = api
        )

        val emissions = repository.getMerriamStat().take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        val success = emissions[1] as ApiResult.Success
        assertEquals(9, success.data.latest)
        assertEquals(listOf(1, 2, null), success.data.weekStats)
    }

    @Test
    fun getMerriamStat_emitsLoadingThenError_whenApiThrows() = runBlocking {
        val api = FakeMerriamApi(
            statError = IOException("no network")
        )
        val repository = MerriamRepository(
            dao = FakeMerriamDao(),
            api = api
        )

        val emissions = repository.getMerriamStat().take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        val error = emissions[1] as ApiResult.Error
        assertEquals("Network error. Please check your connection.", error.message)
    }

    @Test
    fun addMerriamRecords_emitsLoadingThenSuccess_andCallsApi() = runBlocking {
        val api = FakeMerriamApi()
        val repository = MerriamRepository(
            dao = FakeMerriamDao(),
            api = api
        )
        val payload = listOf(RootPostDto(rootId = 100, root = "AM"))

        val emissions = repository.addMerriamRecords(payload).take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        assertTrue(emissions[1] is ApiResult.Success)
        assertEquals(payload, api.postedPayloads.single())
    }

    private class FakeMerriamDao : MerriamDao {
        override fun getAllRootsFlow(): Flow<List<RootEntity>> = flowOf(emptyList())
        override suspend fun getRootsBetween(startExclusive: Int, endInclusive: Int): List<RootEntity> = emptyList()
    }

    private class FakeMerriamApi(
        private val statDto: StatDto = StatDto(0, emptyList()),
        private val statError: Throwable? = null
    ) : MerriamApi {
        val postedPayloads = mutableListOf<List<RootPostDto>>()

        override suspend fun getStat(): StatDto {
            statError?.let { throw it }
            return statDto
        }

        override suspend fun postMerriam(request: List<RootPostDto>) {
            postedPayloads.add(request)
        }
    }
}
