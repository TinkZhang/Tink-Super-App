package app.tinks.tink.zi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZiGameLevelsTest {
    @Test
    fun firstEightStagesFollowSolarSystemPlanetsInOrder() {
        val expectedPlanets = listOf("水星", "金星", "地球", "火星", "木星", "土星", "天王星", "海王星")
        val actual = (1..8).map { ZiGameLevels.stageForNumber(it) }

        assertEquals(expectedPlanets, actual.map { it.name })
        assertTrue(actual.all { it.kind == CelestialKind.Planet })
        assertTrue(actual[5].hasRing) // 土星
        assertTrue(actual[6].hasRing) // 天王星
        assertFalse(actual[0].hasRing) // 水星
    }

    @Test
    fun stagesAfterPlanetsUseFamousSolarSystemDwarfPlanetsAndAsteroids() {
        val asteroidStages = (9..20).map { ZiGameLevels.stageForNumber(it) }
        val expectedNames = listOf(
            "冥王星", "谷神星", "灶神星", "智神星", "婚神星", "龙宫星",
            "贝努星", "爱神星", "灵神星", "阋神星", "妊神星", "鸟神星",
        )

        assertEquals(expectedNames, asteroidStages.map { it.name })
        assertTrue(asteroidStages.all { it.kind == CelestialKind.DwarfOrAsteroid })
    }

    @Test
    fun stagesBeyondCatalogWrapIntoNextCycle() {
        val stage21 = ZiGameLevels.stageForNumber(21)
        assertEquals(21, stage21.number)
        assertEquals("水星 · 第2圈", stage21.name)
        assertEquals("第21关 · 水星 · 第2圈", stage21.displayTitle)
    }

    @Test
    fun stageNumberBelowOneClampsToMercury() {
        val clamped = ZiGameLevels.stageForNumber(0)
        assertEquals(1, clamped.number)
        assertEquals("水星", clamped.name)
    }
}
