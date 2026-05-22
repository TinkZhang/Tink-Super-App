package app.tinks.tink.haircut

import app.tinks.tink.haircut.data.Haircut
import app.tinks.tink.network.ApiResult
import app.tinks.tink.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HaircutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsHistoryAndCalculatesDaysSinceLatestHaircut() = runTest {
        val latestDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .let { LocalDate.fromEpochDays(it.toEpochDays() - 10) }
        val repository = FakeHaircutRepository(
            initialHaircuts = mutableListOf(Haircut(1, 35, latestDate, "Tink Cuts"))
        )

        val viewModel = HaircutViewModel(repository)

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(10, uiState.days)
        assertEquals("Tink Cuts", uiState.history.single().shopName)
        assertEquals(1, repository.getHaircutsCalls)
    }

    @Test
    fun addHaircutFabClick_showsDialogAndDismissHidesIt() = runTest {
        val viewModel = HaircutViewModel(FakeHaircutRepository())

        viewModel.onEvent(HaircutEvent.AddHaircutFabClick)
        assertTrue(viewModel.uiState.value.showDialog)

        viewModel.onEvent(HaircutEvent.DismissDialog)
        assertFalse(viewModel.uiState.value.showDialog)
    }

    @Test
    fun submitHaircut_postsAndRefreshesHistory() = runTest {
        val repository = FakeHaircutRepository()
        val viewModel = HaircutViewModel(repository)
        val date = LocalDate(2026, 5, 22)

        viewModel.onEvent(HaircutEvent.SubmitHaircut(price = 50, shopName = "Fresh Shop", date = date))

        assertEquals(listOf(SubmittedHaircut(50, "Fresh Shop", date)), repository.submittedHaircuts)
        assertFalse(viewModel.uiState.value.showDialog)
        assertEquals("Fresh Shop", viewModel.uiState.value.history.first().shopName)
        assertEquals(2, repository.getHaircutsCalls)
    }

    @Test
    fun deleteHaircut_deletesAndRefreshesHistory() = runTest {
        val repository = FakeHaircutRepository(
            initialHaircuts = mutableListOf(
                Haircut(1, 35, LocalDate(2026, 5, 1), "Old Shop"),
                Haircut(2, 45, LocalDate(2026, 5, 20), "New Shop"),
            )
        )
        val viewModel = HaircutViewModel(repository)

        viewModel.onEvent(HaircutEvent.DeleteHaircut(1))

        assertEquals(listOf(1), repository.deletedIds)
        assertEquals(listOf(2), viewModel.uiState.value.history.map { it.id })
    }

    private data class SubmittedHaircut(
        val price: Int,
        val shopName: String,
        val date: LocalDate,
    )

    private class FakeHaircutRepository(
        initialHaircuts: MutableList<Haircut> = mutableListOf(),
    ) : HaircutRepository(NoopHaircutApi) {
        private val haircuts = initialHaircuts
        val submittedHaircuts = mutableListOf<SubmittedHaircut>()
        val deletedIds = mutableListOf<Int>()
        var getHaircutsCalls = 0

        override fun getHaircuts(): Flow<ApiResult<List<Haircut>>> {
            getHaircutsCalls += 1
            return flowOf(ApiResult.Loading, ApiResult.Success(haircuts.sortedByDescending { it.date }))
        }

        override fun addHaircut(price: Int, shopName: String, date: LocalDate): Flow<ApiResult<Haircut>> {
            submittedHaircuts.add(SubmittedHaircut(price, shopName, date))
            val haircut = Haircut(
                id = (haircuts.maxOfOrNull { it.id ?: 0 } ?: 0) + 1,
                price = price,
                date = date,
                shopName = shopName,
            )
            haircuts.add(haircut)
            return flowOf(ApiResult.Loading, ApiResult.Success(haircut))
        }

        override fun deleteHaircut(id: Int): Flow<ApiResult<Unit>> {
            deletedIds.add(id)
            haircuts.removeAll { it.id == id }
            return flowOf(ApiResult.Loading, ApiResult.Success(Unit))
        }
    }

    private object NoopHaircutApi : HaircutApi {
        override suspend fun getHaircuts(): List<HaircutDto> = emptyList()
        override suspend fun createHaircut(payload: HaircutCreateRequest): HaircutDto = error("not used")
        override suspend fun deleteHaircut(haircutId: Int) = Unit
    }
}
