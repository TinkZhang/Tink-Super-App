package app.tinks.tink.time

import app.tinks.tink.network.ApiResult
import app.tinks.tink.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class TimeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_usesWorkingAsDefaultTypeAndLoadsLabels() = runTest {
        val repository = FakeTimeRepository(
            labels = mutableListOf(TimeLabel(id = 1, type = DEFAULT_TIME_TYPE, name = "Planning", sortOrder = 0)),
        )

        val viewModel = TimeViewModel(repository)

        assertEquals(DEFAULT_TIME_TYPE, viewModel.uiState.value.editor.type)
        assertEquals(listOf("Planning"), viewModel.uiState.value.labels.map { it.name })
        assertEquals(1, repository.dashboardCalls)
        assertEquals(1, repository.labelCalls)
    }

    @Test
    fun dashboard_filtersEntriesToSelectedDateRangeOnClient() = runTest {
        val selectedDay = LocalDate.parse("2026-05-22")
        val repository = FakeTimeRepository(
            dashboard = TimeDashboard(
                statistics = listOf(TimeStatistic(type = 5, duration = 60)),
                entries = listOf(
                    sampleEntry(id = 1, start = "2026-05-22T09:00:00Z"),
                    sampleEntry(id = 2, start = "2026-05-23T09:00:00Z"),
                ),
            )
        )
        val viewModel = TimeViewModel(repository)

        viewModel.onEvent(TimeEvent.UpdateStartDate(selectedDay))
        viewModel.onEvent(TimeEvent.UpdateEndDate(selectedDay))

        val entries = viewModel.uiState.value.entriesByDay.flatMap { it.entries }
        assertEquals(listOf(1L), entries.map { it.id })
        assertEquals(selectedDay, viewModel.uiState.value.entriesByDay.single().day)
    }

    @Test
    fun applyLabel_prefixesTitleAndReplacesExistingKnownLabel() = runTest {
        val planning = TimeLabel(id = 1, type = 5, name = "Planning", sortOrder = 0)
        val coding = TimeLabel(id = 2, type = 5, name = "Coding", sortOrder = 1)
        val viewModel = TimeViewModel(FakeTimeRepository(labels = mutableListOf(planning, coding)))

        viewModel.onEvent(TimeEvent.OpenAddDialog)
        viewModel.onEvent(TimeEvent.UpdateTitle("Build report"))
        viewModel.onEvent(TimeEvent.ApplyLabel(planning))
        assertEquals("Planning: Build report", viewModel.uiState.value.editor.title)

        viewModel.onEvent(TimeEvent.ApplyLabel(coding))
        assertEquals("Coding: Build report", viewModel.uiState.value.editor.title)
    }

    @Test
    fun saveEntry_usesPrefixedTitleAndRefreshesDashboard() = runTest {
        val repository = FakeTimeRepository()
        val viewModel = TimeViewModel(repository)

        viewModel.onEvent(TimeEvent.OpenAddDialog)
        viewModel.onEvent(TimeEvent.UpdateTitle("Coding: Build report"))
        viewModel.onEvent(TimeEvent.SaveEntry)

        assertEquals("Coding: Build report", repository.createdEntries.single().title)
        assertFalse(viewModel.uiState.value.showEditor)
        assertTrue(repository.dashboardCalls >= 2)
    }

    @Test
    fun saveLabel_addsLabelToState() = runTest {
        val repository = FakeTimeRepository()
        val viewModel = TimeViewModel(repository)

        viewModel.onEvent(TimeEvent.OpenLabelManager)
        viewModel.onEvent(TimeEvent.UpdateLabelDraft("Meeting"))
        viewModel.onEvent(TimeEvent.SaveLabel)

        assertEquals(listOf("Meeting"), viewModel.uiState.value.labels.map { it.name })
        assertEquals("", viewModel.uiState.value.labelManager.draftName)
    }

    private class FakeTimeRepository(
        private val dashboard: TimeDashboard = TimeDashboard(statistics = emptyList(), entries = emptyList()),
        labels: MutableList<TimeLabel> = mutableListOf(),
    ) : TimeRepository(NoopTimeApi) {
        private val labelStore = labels
        val createdEntries = mutableListOf<TimeUpsertRequest>()
        var dashboardCalls = 0
        var labelCalls = 0

        override fun getTimeDashboard(startDate: LocalDate, endDate: LocalDate): Flow<ApiResult<TimeDashboard>> {
            dashboardCalls += 1
            return flowOf(ApiResult.Loading, ApiResult.Success(dashboard))
        }

        override fun getTimeLabels(type: Int?): Flow<ApiResult<List<TimeLabel>>> {
            labelCalls += 1
            return flowOf(ApiResult.Loading, ApiResult.Success(labelStore.filter { type == null || it.type == type }))
        }

        override fun createTimeEntry(payload: TimeUpsertRequest): Flow<ApiResult<Unit>> {
            createdEntries.add(payload)
            return flowOf(ApiResult.Loading, ApiResult.Success(Unit))
        }

        override fun createTimeLabel(type: Int, name: String): Flow<ApiResult<TimeLabel>> {
            val label = TimeLabel(
                id = (labelStore.maxOfOrNull { it.id } ?: 0L) + 1L,
                type = type,
                name = name.trim(),
                sortOrder = 0,
            )
            labelStore.add(label)
            return flowOf(ApiResult.Loading, ApiResult.Success(label))
        }
    }

    private object NoopTimeApi : TimeApi {
        override suspend fun getStatistics(startDate: String, endDate: String): List<TimeStatisticDto> = emptyList()
        override suspend fun getTimeEntries(startDate: String, endDate: String): List<TimeEntryDto> = emptyList()
        override suspend fun createTimeEntry(payload: TimeUpsertRequest) = Unit
        override suspend fun updateTimeEntry(timeId: Long, payload: TimeUpsertRequest) = Unit
        override suspend fun deleteTimeEntry(timeId: Long) = Unit
        override suspend fun getTimeLabels(type: Int?): List<TimeLabelDto> = emptyList()
        override suspend fun createTimeLabel(payload: TimeLabelCreateRequest): TimeLabelDto = error("not used")
        override suspend fun updateTimeLabel(labelId: Long, payload: TimeLabelUpdateRequest): TimeLabelDto = error("not used")
        override suspend fun deleteTimeLabel(labelId: Long) = Unit
    }
}

private fun sampleEntry(
    id: Long,
    start: String,
): TimeEntry = TimeEntry(
    id = id,
    type = 5,
    start = OffsetDateTime.parse(start),
    end = OffsetDateTime.parse(start).plusHours(1),
    title = "Working",
    description = null,
)
