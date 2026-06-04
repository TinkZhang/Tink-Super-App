package app.tinks.tink.lottery

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import java.time.Instant

interface LotteryApi {
    @GET("lottery")
    suspend fun getLotteryHistory(): List<LotteryHistoryDto>

    @POST("lottery")
    suspend fun createLottery(
        @Body payload: LotteryCreateRequest,
    ): LotteryHistoryDto

    @GET("lottery/{lotteryId}")
    suspend fun getLottery(
        @Path("lotteryId") lotteryId: Int,
    ): LotteryHistoryDto

    @PATCH("lottery/{lotteryId}")
    suspend fun updateLottery(
        @Path("lotteryId") lotteryId: Int,
        @Body payload: LotteryUpdateRequest,
    ): LotteryHistoryDto

    @DELETE("lottery/{lotteryId}")
    suspend fun deleteLottery(
        @Path("lotteryId") lotteryId: Int,
    )

    @POST("lottery/{lotteryId}/check")
    suspend fun checkLottery(
        @Path("lotteryId") lotteryId: Int,
    ): LotteryCheckResponseDto

    @GET("lottery-results/{issueId}")
    suspend fun getLotteryResult(
        @Path("issueId") issueId: String,
    ): LotteryResultDto
}

@Serializable
data class LotteryHistoryDto(
    val id: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val type: String,
    @SerialName("issue_id")
    val issueId: String,
    @SerialName("front_numbers")
    val frontNumbers: List<Int>,
    @SerialName("back_numbers")
    val backNumbers: List<Int>,
    @SerialName("reveal_time")
    val revealTime: String,
    @SerialName("captured_image_uri")
    val capturedImageUri: String? = null,
    val checked: Boolean,
    @SerialName("checked_at")
    val checkedAt: String? = null,
    @SerialName("result_id")
    val resultId: Int? = null,
    @SerialName("prize_tier")
    val prizeTier: String? = null,
    @SerialName("front_match_count")
    val frontMatchCount: Int? = null,
    @SerialName("back_match_count")
    val backMatchCount: Int? = null,
    val result: LotteryResultDto? = null,
)

@Serializable
data class LotteryResultDto(
    val id: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val type: String,
    @SerialName("issue_id")
    val issueId: String,
    @SerialName("front_numbers")
    val frontNumbers: List<Int>,
    @SerialName("back_numbers")
    val backNumbers: List<Int>,
    @SerialName("opened_at")
    val openedAt: String? = null,
    val source: String,
)

@Serializable
data class LotteryCreateRequest(
    val type: String = LOTTERY_TYPE_DA_LE_TOU,
    @SerialName("issue_id")
    val issueId: String,
    @SerialName("front_numbers")
    val frontNumbers: List<Int>,
    @SerialName("back_numbers")
    val backNumbers: List<Int>,
    @SerialName("reveal_time")
    val revealTime: String,
    @SerialName("captured_image_uri")
    val capturedImageUri: String? = null,
)

@Serializable
data class LotteryUpdateRequest(
    val type: String? = null,
    @SerialName("issue_id")
    val issueId: String? = null,
    @SerialName("front_numbers")
    val frontNumbers: List<Int>? = null,
    @SerialName("back_numbers")
    val backNumbers: List<Int>? = null,
    @SerialName("reveal_time")
    val revealTime: String? = null,
    @SerialName("captured_image_uri")
    val capturedImageUri: String? = null,
)

@Serializable
data class LotteryCheckResponseDto(
    val lottery: LotteryHistoryDto,
    val result: LotteryResultDto,
    @SerialName("front_match_count")
    val frontMatchCount: Int,
    @SerialName("back_match_count")
    val backMatchCount: Int,
    @SerialName("prize_tier")
    val prizeTier: String,
)

fun LotteryHistoryDto.toDomain(): LotteryTicket =
    LotteryTicket(
        id = id,
        type = type,
        issueId = issueId,
        numbers = LotteryNumbers(frontNumbers.sorted(), backNumbers.sorted()),
        revealTime = parseInstantOrEpoch(revealTime),
        capturedImageUri = capturedImageUri,
        checked = checked,
        checkedAt = checkedAt?.let(::parseInstantOrNull),
        resultId = resultId,
        prizeTier = prizeTier,
        frontMatchCount = frontMatchCount,
        backMatchCount = backMatchCount,
        result = result?.toDomain(),
    )

fun LotteryResultDto.toDomain(): LotteryResult =
    LotteryResult(
        id = id,
        type = type,
        issueId = issueId,
        numbers = LotteryNumbers(frontNumbers.sorted(), backNumbers.sorted()),
        openedAt = openedAt?.let(::parseInstantOrNull),
        source = source,
    )

fun LotteryCheckResponseDto.toDomain(): LotteryCheckOutcome =
    LotteryCheckOutcome(
        lottery = lottery.toDomain(),
        result = result.toDomain(),
        frontMatchCount = frontMatchCount,
        backMatchCount = backMatchCount,
        prizeTier = prizeTier,
    )

private fun parseInstantOrEpoch(value: String): Instant =
    parseInstantOrNull(value) ?: Instant.EPOCH

private fun parseInstantOrNull(value: String): Instant? =
    runCatching { Instant.parse(value) }.getOrNull()
