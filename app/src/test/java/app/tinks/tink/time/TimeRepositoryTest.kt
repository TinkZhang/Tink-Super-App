package app.tinks.tink.time

import app.tinks.tink.network.ApiResult
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TimeRepositoryTest {

    @Test
    fun getTimeDashboard_usesSelectedDateRangeForStatisticsAndEntries() = runTest {
        val api = FakeTimeApi(
            statistics = listOf(TimeStatisticDto(type = 5, duration = 90)),
            entries = listOf(sampleEntryDto(id = 1, start = "2026-05-22T09:00:00+08:00")),
        )
        val repository = TimeRepository(api)

        val emissions = repository.getTimeDashboard(
            startDate = LocalDate.parse("2026-05-22"),
            endDate = LocalDate.parse("2026-05-23"),
        ).take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        val success = emissions[1] as ApiResult.Success
        assertEquals(listOf(5), success.data.statistics.map { it.type })
        assertEquals(listOf(1L), success.data.entries.map { it.id })
        assertEquals("2026-05-22" to "2026-05-23", api.statisticsRange)
        assertEquals("2026-05-22" to "2026-05-23", api.entriesRange)
    }

    @Test
    fun createTimeEntry_postsPayload() = runTest {
        val api = FakeTimeApi()
        val repository = TimeRepository(api)
        val payload = TimeUpsertRequest(
            type = 5,
            start = "2026-05-22T01:00:00Z",
            end = "2026-05-22T02:00:00Z",
            title = "Work",
        )

        val emissions = repository.createTimeEntry(payload).take(2).toList()

        assertTrue(emissions[0] is ApiResult.Loading)
        assertTrue(emissions[1] is ApiResult.Success)
        assertEquals(payload, api.createdEntries.single())
    }

    @Test
    fun timeLabelCrud_callsLabelEndpoints() = runTest {
        val api = FakeTimeApi(labels = mutableListOf(TimeLabelDto(id = 1, type = 5, name = "Planning")))
        val repository = TimeRepository(api)

        val labels = repository.getTimeLabels(type = 5).take(2).toList()[1] as ApiResult.Success
        val created = repository.createTimeLabel(type = 5, name = " Coding ").take(2).toList()[1] as ApiResult.Success
        val updated = repository.updateTimeLabel(labelId = 1, type = 5, name = "Deep work").take(2).toList()[1] as ApiResult.Success
        val deleted = repository.deleteTimeLabel(labelId = 1).take(2).toList()[1]

        assertEquals(listOf("Planning"), labels.data.map { it.name })
        assertEquals("Coding", created.data.name)
        assertEquals("Deep work", updated.data.name)
        assertTrue(deleted is ApiResult.Success)
        assertEquals(listOf(1L), api.deletedLabelIds)
    }

    private class FakeTimeApi(
        private val statistics: List<TimeStatisticDto> = emptyList(),
        private val entries: List<TimeEntryDto> = emptyList(),
        private val labels: MutableList<TimeLabelDto> = mutableListOf(),
    ) : TimeApi {
        var statisticsRange: Pair<String, String>? = null
        var entriesRange: Pair<String, String>? = null
        val createdEntries = mutableListOf<TimeUpsertRequest>()
        val deletedLabelIds = mutableListOf<Long>()

        override suspend fun getStatistics(startDate: String, endDate: String): List<TimeStatisticDto> {
            statisticsRange = startDate to endDate
            return statistics
        }

        override suspend fun getTimeEntries(startDate: String, endDate: String): List<TimeEntryDto> {
            entriesRange = startDate to endDate
            return entries
        }

        override suspend fun createTimeEntry(payload: TimeUpsertRequest): TimeEntryDto {
            createdEntries.add(payload)
            return sampleEntryDto(id = 99, start = payload.start, end = payload.end, title = payload.title)
        }

        override suspend fun updateTimeEntry(timeId: Long, payload: TimeUpsertRequest): TimeEntryDto =
            sampleEntryDto(id = timeId, start = payload.start, end = payload.end, title = payload.title)

        override suspend fun deleteTimeEntry(timeId: Long) = Unit

        override suspend fun getTimeLabels(type: Int?): List<TimeLabelDto> =
            labels.filter { type == null || it.type == type }

        override suspend fun createTimeLabel(payload: TimeLabelCreateRequest): TimeLabelDto {
            val label = TimeLabelDto(
                id = (labels.maxOfOrNull { it.id } ?: 0L) + 1L,
                type = payload.type,
                name = payload.name.trim(),
                sortOrder = payload.sortOrder,
            )
            labels.add(label)
            return label
        }

        override suspend fun updateTimeLabel(labelId: Long, payload: TimeLabelUpdateRequest): TimeLabelDto {
            val current = labels.first { it.id == labelId }
            val updated = current.copy(
                type = payload.type ?: current.type,
                name = payload.name?.trim() ?: current.name,
                sortOrder = payload.sortOrder ?: current.sortOrder,
            )
            labels.removeAll { it.id == labelId }
            labels.add(updated)
            return updated
        }

        override suspend fun deleteTimeLabel(labelId: Long) {
            deletedLabelIds.add(labelId)
            labels.removeAll { it.id == labelId }
        }
    }
}

private fun sampleEntryDto(
    id: Long,
    type: Int = 5,
    start: String,
    end: String = "2026-05-22T10:00:00+08:00",
    title: String = "Working",
): TimeEntryDto = TimeEntryDto(
    id = id,
    type = type,
    start = start,
    end = end,
    title = title,
)
