package app.tinks.tink.random

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class RandomModelsTest {
    @Test fun weightedTickets_haveExactConfiguredProportions() {
        val sections = listOf(WheelSection("A", 1), WheelSection("B", 3), WheelSection("C", 2))
        assertEquals(listOf(0, 1, 1, 1, 2, 2), (0..5).map { weightedIndex(sections, it) })
    }
    @Test fun pointerLandsInsideSelectedSector_afterRepeatedSpins() {
        val sections = RandomSettings().sections
        var rotation = 0f
        listOf(0, 3, 1, 1, 2).forEach { index ->
            val next = nextWheelRotation(rotation, sections, index)
            assertTrue(next - rotation >= 1800f)
            assertEquals(0f, (next + wheelCenterDegrees(sections, index)) % 360f, .01f)
            rotation = next
        }
    }
    @Test fun diceRespectCountAndBounds_andReportChineseTotal() {
        (1..6).forEach { count ->
            val settings = RandomSettings(diceCount = count)
            repeat(100) { val draw = drawRandom(settings, Random(it)); assertEquals(count, draw.dice.size); assertTrue(draw.dice.all { face -> face in 1..6 }) }
        }
        assertEquals("六点、六点，共十二点", RandomResult(dice = listOf(6, 6)).chinese(RandomSettings()))
        assertEquals("三十六", chineseNumber(36))
        assertEquals("反面", RandomResult(heads = false).chinese(RandomSettings(type = RandomType.Coin)))
    }
    @Test fun settingsRejectInvalidAndEmptyWheel() {
        assertFalse(RandomSettings(sections = emptyList()).isValid())
        assertFalse(RandomSettings(sections = listOf(WheelSection("", 0), WheelSection("A"))).isValid())
        assertFalse(RandomSettings(diceCount = 7).isValid())
        assertTrue(RandomSettings().isValid())
    }
}
