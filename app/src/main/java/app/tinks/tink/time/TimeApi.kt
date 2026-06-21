package app.tinks.tink.time

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

interface TimeApi {
    @GET("time/statistics")
    suspend fun getStatistics(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
    ): List<TimeStatisticDto>

    @GET("time")
    suspend fun getTimeEntries(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
    ): List<TimeEntryDto>

    @POST("time")
    suspend fun createTimeEntry(
        @Body payload: TimeUpsertRequest,
    ): TimeEntryDto

    @PATCH("time/{timeId}")
    suspend fun updateTimeEntry(
        @Path("timeId") timeId: Long,
        @Body payload: TimeUpsertRequest,
    ): TimeEntryDto

    @DELETE("time/{timeId}")
    suspend fun deleteTimeEntry(
        @Path("timeId") timeId: Long,
    )

    @GET("time/labels")
    suspend fun getTimeLabels(
        @Query("type") type: Int? = null,
    ): List<TimeLabelDto>

    @POST("time/labels")
    suspend fun createTimeLabel(
        @Body payload: TimeLabelCreateRequest,
    ): TimeLabelDto

    @PATCH("time/labels/{labelId}")
    suspend fun updateTimeLabel(
        @Path("labelId") labelId: Long,
        @Body payload: TimeLabelUpdateRequest,
    ): TimeLabelDto

    @DELETE("time/labels/{labelId}")
    suspend fun deleteTimeLabel(
        @Path("labelId") labelId: Long,
    )
}

@Serializable
data class TimeStatisticDto(
    val type: Int,
    val duration: Long,
)

@Serializable
data class TimeEntryDto(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("time_id")
    val timeId: Long? = null,
    val type: Int,
    val start: String,
    val end: String,
    val title: String,
    val description: String? = null,
    @SerialName("all_day")
    val allDay: Boolean = false,
    @SerialName("include_in_statistics")
    val includeInStatistics: Boolean = true,
    val source: String = "time",
)

@Serializable
data class TimeUpsertRequest(
    val type: Int,
    val start: String,
    val end: String,
    val title: String,
    val description: String? = null,
    @SerialName("all_day")
    val allDay: Boolean = false,
    @SerialName("include_in_statistics")
    val includeInStatistics: Boolean = true,
    val source: String = "time",
)

@Serializable
data class TimeLabelDto(
    val id: Long,
    val type: Int,
    val name: String,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
)

@Serializable
data class TimeLabelCreateRequest(
    val type: Int,
    val name: String,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
)

@Serializable
data class TimeLabelUpdateRequest(
    val type: Int? = null,
    val name: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int? = null,
)

data class TimeStatistic(
    val type: Int,
    val duration: Long,
)

data class TimeEntry(
    val id: Long,
    val type: Int,
    val start: OffsetDateTime,
    val end: OffsetDateTime,
    val title: String,
    val description: String?,
    val allDay: Boolean = false,
    val includeInStatistics: Boolean = true,
    val source: String = "time",
)

data class TimeDashboard(
    val statistics: List<TimeStatistic>,
    val entries: List<TimeEntry>,
)

data class TimeLabel(
    val id: Long,
    val type: Int,
    val name: String,
    val sortOrder: Int,
)

fun TimeStatisticDto.toDomain(): TimeStatistic = TimeStatistic(
    type = type,
    duration = duration,
)

fun TimeEntryDto.toDomain(): TimeEntry = TimeEntry(
    id = id ?: timeId ?: 0L,
    type = type,
    start = parseOffsetDateTime(start),
    end = parseOffsetDateTime(end),
    title = title,
    description = description,
    allDay = allDay,
    includeInStatistics = includeInStatistics,
    source = source,
)

fun TimeLabelDto.toDomain(): TimeLabel = TimeLabel(
    id = id,
    type = type,
    name = name,
    sortOrder = sortOrder,
)

private fun parseOffsetDateTime(value: String): OffsetDateTime {
    return runCatching { OffsetDateTime.parse(value) }
        .getOrElse {
            runCatching { Instant.parse(value).atOffset(ZoneOffset.UTC) }
                .getOrElse { Instant.EPOCH.atOffset(ZoneOffset.UTC) }
        }
}
