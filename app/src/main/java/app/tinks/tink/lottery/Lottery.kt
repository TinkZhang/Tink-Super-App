package app.tinks.tink.lottery

import java.time.Instant

const val LOTTERY_TYPE_DA_LE_TOU = "超级大乐透"

data class LotteryNumbers(
    val front: List<Int>,
    val back: List<Int>,
)

data class LotteryResult(
    val id: Int,
    val type: String,
    val issueId: String,
    val numbers: LotteryNumbers,
    val openedAt: Instant?,
    val source: String,
)

data class LotteryTicket(
    val id: Int,
    val type: String,
    val issueId: String,
    val numbers: LotteryNumbers,
    val revealTime: Instant,
    val capturedImageUri: String?,
    val checked: Boolean,
    val checkedAt: Instant?,
    val resultId: Int?,
    val prizeTier: String?,
    val frontMatchCount: Int?,
    val backMatchCount: Int?,
    val result: LotteryResult?,
)

data class LotteryCheckOutcome(
    val lottery: LotteryTicket,
    val result: LotteryResult,
    val frontMatchCount: Int,
    val backMatchCount: Int,
    val prizeTier: String,
)

data class LotteryDraft(
    val type: String = LOTTERY_TYPE_DA_LE_TOU,
    val issueId: String = "",
    val frontNumbersText: String = "",
    val backNumbersText: String = "",
    val revealTimeText: String = "",
    val capturedImageUri: String? = null,
    val parseError: String? = null,
) {
    val jsonPreview: String
        get() = """
            {
              "type": "$type",
              "issue_id": "$issueId",
              "front_numbers": [${frontNumbersText.toJsonNumbers()}],
              "back_numbers": [${backNumbersText.toJsonNumbers()}],
              "reveal_time": "$revealTimeText"
            }
        """.trimIndent()
}

private fun String.toJsonNumbers(): String =
    split(Regex("[,，\\s]+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(", ") { it.toIntOrNull()?.toString() ?: "\"$it\"" }

object LotteryPrizeClassifier {
    fun classify(frontMatches: Int, backMatches: Int): String =
        when {
            frontMatches == 5 && backMatches == 2 -> "一等奖"
            frontMatches == 5 && backMatches == 1 -> "二等奖"
            frontMatches == 5 && backMatches == 0 -> "三等奖"
            frontMatches == 4 && backMatches == 2 -> "四等奖"
            frontMatches == 4 && backMatches == 1 -> "五等奖"
            frontMatches == 3 && backMatches == 2 -> "六等奖"
            frontMatches == 4 && backMatches == 0 -> "七等奖"
            frontMatches == 3 && backMatches == 1 -> "八等奖"
            frontMatches == 2 && backMatches == 2 -> "八等奖"
            frontMatches == 3 && backMatches == 0 -> "九等奖"
            frontMatches == 2 && backMatches == 1 -> "九等奖"
            frontMatches == 1 && backMatches == 2 -> "九等奖"
            frontMatches == 0 && backMatches == 2 -> "九等奖"
            else -> "未中奖"
        }

    fun classify(ticket: LotteryNumbers, result: LotteryNumbers): LotteryMatchSummary {
        val frontMatches = ticket.front.toSet().intersect(result.front.toSet()).size
        val backMatches = ticket.back.toSet().intersect(result.back.toSet()).size
        return LotteryMatchSummary(
            frontMatches = frontMatches,
            backMatches = backMatches,
            prizeTier = classify(frontMatches, backMatches),
        )
    }
}

data class LotteryMatchSummary(
    val frontMatches: Int,
    val backMatches: Int,
    val prizeTier: String,
) {
    val isWinning: Boolean get() = prizeTier != "未中奖"
}

fun LotteryTicket.matchSummary(): LotteryMatchSummary? {
    val front = frontMatchCount ?: return null
    val back = backMatchCount ?: return null
    return LotteryMatchSummary(front, back, prizeTier ?: LotteryPrizeClassifier.classify(front, back))
}

fun LotteryDraft.toCreateRequest(): LotteryCreateRequest {
    val frontNumbers = frontNumbersText.parseNumbers(expectedCount = 5, min = 1, max = 35)
    val backNumbers = backNumbersText.parseNumbers(expectedCount = 2, min = 1, max = 12)
    val revealTime = runCatching { Instant.parse(revealTimeText.trim()) }
        .getOrElse { throw IllegalArgumentException("Reveal time must be an ISO instant, such as 2026-06-04T12:30:00Z.") }
    val cleanIssueId = issueId.trim().removePrefix("第").removeSuffix("期")
    require(cleanIssueId.isNotBlank()) { "Issue id is required." }

    return LotteryCreateRequest(
        type = type.ifBlank { LOTTERY_TYPE_DA_LE_TOU },
        issueId = cleanIssueId,
        frontNumbers = frontNumbers,
        backNumbers = backNumbers,
        revealTime = revealTime.toString(),
        capturedImageUri = capturedImageUri,
    )
}

private fun String.parseNumbers(expectedCount: Int, min: Int, max: Int): List<Int> {
    val numbers = split(Regex("[,，\\s]+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { token ->
            token.toIntOrNull() ?: throw IllegalArgumentException("Numbers must be numeric.")
        }
    require(numbers.size == expectedCount) { "Expected $expectedCount numbers." }
    require(numbers.distinct().size == numbers.size) { "Numbers cannot repeat." }
    require(numbers.all { it in min..max }) { "Numbers must be between $min and $max." }
    return numbers.sorted()
}
