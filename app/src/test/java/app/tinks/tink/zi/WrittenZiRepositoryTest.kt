package app.tinks.tink.zi

import app.tinks.tink.network.ApiResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WrittenZiRepositoryTest {
    @Test fun savedCharacterUpdatesTheCachedListOnlyOnce() = runTest {
        val api = FakeZiApi()
        val repository = WrittenZiRepository(api)
        repository.refresh()

        assertEquals(ApiResult.Success(WrittenZiResult(true)), repository.save("土"))
        assertEquals(ApiResult.Success(WrittenZiResult(false)), repository.save("土"))
        assertEquals(listOf("土"), repository.state.value.characters)
    }

    @Test fun failedRefreshRetainsThePreviouslyLoadedList() = runTest {
        val api = FakeZiApi().apply { written += "山" }
        val repository = WrittenZiRepository(api)
        repository.refresh()
        api.fail = true

        repository.refresh()

        assertEquals(listOf("山"), repository.state.value.characters)
        assertFalse(repository.state.value.isLoading)
        assertTrue(repository.state.value.failed)
    }

    @Test fun batchStopsAtFailureAndCountsExistingCharactersAsConfirmed() = runTest {
        val api = FakeZiApi().apply {
            written += "山"
            rejectedCharacter = "土"
        }
        val repository = WrittenZiRepository(api)
        repository.refresh()

        assertEquals(WrittenZiBatchResult(2, 1), repository.saveBatch(listOf("山", "木", "土", "水")))
        assertEquals(listOf("木", "山").toSet(), repository.state.value.characters?.toSet())
        assertEquals(listOf("山", "木", "土"), api.requested)

        api.rejectedCharacter = null
        assertEquals(WrittenZiBatchResult(2, 2), repository.saveBatch(listOf("土", "水")))
        assertEquals(setOf("山", "木", "土", "水"), repository.state.value.characters?.toSet())
    }

    private class FakeZiApi : ZiApi {
        val written = linkedSetOf<String>()
        var fail = false
        var rejectedCharacter: String? = null
        val requested = mutableListOf<String>()

        override suspend fun getWrittenZis(): List<String> {
            check(!fail)
            return written.toList()
        }

        override suspend fun putWrittenZi(request: WrittenZiRequest): WrittenZiResult {
            requested += request.zi
            check(request.zi != rejectedCharacter)
            return WrittenZiResult(written.add(request.zi))
        }
        override suspend fun getZis(size: Int?, proficiency: Int?): List<ZiDto> = error("Unexpected reading request")
        override suspend fun putZi(request: PutZiRequest): Unit = error("Unexpected reading write")
        override suspend fun getLearntZiNum(): Int = error("Unexpected reading count request")
        override suspend fun generateStory(request: StoryGenerateRequest): Unit = error("Unexpected story request")
    }
}
