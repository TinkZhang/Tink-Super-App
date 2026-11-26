package app.tinks.tink.lottery

import org.junit.Assert.assertEquals
import org.junit.Test

class LotteryPrizeClassifierTest {

    @Test
    fun classify_coversDaLeTouPrizeTiers() {
        assertEquals("一等奖", LotteryPrizeClassifier.classify(5, 2))
        assertEquals("二等奖", LotteryPrizeClassifier.classify(5, 1))
        assertEquals("三等奖", LotteryPrizeClassifier.classify(5, 0))
        assertEquals("四等奖", LotteryPrizeClassifier.classify(4, 2))
        assertEquals("五等奖", LotteryPrizeClassifier.classify(4, 1))
        assertEquals("六等奖", LotteryPrizeClassifier.classify(3, 2))
        assertEquals("七等奖", LotteryPrizeClassifier.classify(4, 0))
        assertEquals("八等奖", LotteryPrizeClassifier.classify(3, 1))
        assertEquals("八等奖", LotteryPrizeClassifier.classify(2, 2))
        assertEquals("九等奖", LotteryPrizeClassifier.classify(3, 0))
        assertEquals("九等奖", LotteryPrizeClassifier.classify(2, 1))
        assertEquals("九等奖", LotteryPrizeClassifier.classify(1, 2))
        assertEquals("九等奖", LotteryPrizeClassifier.classify(0, 2))
        assertEquals("未中奖", LotteryPrizeClassifier.classify(1, 1))
    }

    @Test
    fun classify_countsUnorderedNumberMatches() {
        val ticket = LotteryNumbers(front = listOf(35, 1, 11, 12, 34), back = listOf(12, 9))
        val result = LotteryNumbers(front = listOf(1, 11, 12, 34, 35), back = listOf(9, 12))

        val summary = LotteryPrizeClassifier.classify(ticket, result)

        assertEquals(5, summary.frontMatches)
        assertEquals(2, summary.backMatches)
        assertEquals("一等奖", summary.prizeTier)
    }

    @Test
    fun matchedNumbers_onlyCompareWithinEachArea() {
        val ticket = LotteryNumbers(front = listOf(1, 3, 8, 12, 35), back = listOf(2, 9))
        val winning = LotteryNumbers(front = listOf(1, 2, 4, 11, 20), back = listOf(8, 9))

        assertEquals(LotteryNumbers(front = listOf(1), back = listOf(9)), ticket.matchedWith(winning))
        val summary = LotteryPrizeClassifier.classify(ticket, winning)
        assertEquals(1, summary.frontMatches)
        assertEquals(1, summary.backMatches)
    }
}
