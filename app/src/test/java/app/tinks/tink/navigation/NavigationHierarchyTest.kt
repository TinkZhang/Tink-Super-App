package app.tinks.tink.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationHierarchyTest {

    @Test
    fun topDestination_returnsSelf_forTopLevelDestinations() {
        assertEquals(ScreenWeight, ScreenWeight.topDestination())
        assertEquals(ScreenTime, ScreenTime.topDestination())
    }

    @Test
    fun topDestination_mapsWeightHistoryToWeight() {
        assertEquals(ScreenWeight, ScreenWeightHistory.topDestination())
    }

    @Test
    fun topDestination_mapsLotteryHistoryStatsToLottery() {
        assertEquals(ScreenLottery, ScreenLotteryHistoryStats.topDestination())
    }

    @Test
    fun topDestination_mapsStoryFlowToZi() {
        assertEquals(ScreenZi, ScreenStoryList.topDestination())
        assertEquals(ScreenZi, ScreenStoryDetail("story-id").topDestination())
    }
}
