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
    )

    @PATCH("time/{timeId}")
    suspend fun updateTimeEntry(
        @Path("timeId") timeId: Long,
        @Body payload: TimeUpsertRequest,
    )

    @DELETE("time/{timeId}")
    suspend fun deleteTimeEntry(
        @Path("timeId") timeId: Long,
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
)

@Serializable
data class TimeUpsertRequest(
    val type: Int,
    val start: String,
    val end: String,
    val title: String,
    val description: String? = null,
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
)

data class TimeDashboard(
    val statistics: List<TimeStatistic>,
    val entries: List<TimeEntry>,
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
)

private fun parseOffsetDateTime(value: String): OffsetDateTime {
    return runCatching { OffsetDateTime.parse(value) }
        .getOrElse {
            runCatching { Instant.parse(value).atOffset(ZoneOffset.UTC) }
                .getOrElse { Instant.EPOCH.atOffset(ZoneOffset.UTC) }
        }
}
