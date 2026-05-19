package app.tinks.tink.weight

import app.tinks.tink.weight.data.Weight
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightStateTest {

    @Test
    fun toUiState_mapsCurrentWeightAndHistory() {
        val today = LocalDate.now().toEpochMillis()
        val weight = Weight(id = 1, weight = 141.2, createdTime = today)
        val state = WeightState(
            lastWeight = weight,
            newWeight = 141.2,
            allWeights = listOf(weight),
            isLoading = false,
            isWeightChanged = false,
        )

        val uiState = state.toUiState()

        assertFalse(uiState.isLoading)
        assertTrue(uiState.weightControlCardUiState.isTodayRecorded)
        assertEquals(141.2, uiState.weightControlCardUiState.newWeight)
        assertEquals(listOf(weight), uiState.allWeights)
    }

    @Test
    fun toUiState_filtersTrendWeightsToCurrentMonthByDefault() {
        val thisMonth = Weight(id = 1, weight = 141.0, createdTime = LocalDate.now().toEpochMillis())
        val previousMonth = Weight(
            id = 2,
            weight = 142.0,
            createdTime = LocalDate.now().minusMonths(1).toEpochMillis(),
        )
        val state = WeightState(
            allWeights = listOf(previousMonth, thisMonth),
            selectedIndex = 0,
        )

        val trendWeights = state.toUiState().trendChartCardUiState.weightList

        assertEquals(listOf(thisMonth), trendWeights)
    }

    @Test
    fun toUiState_returnsAllTrendWeightsSortedByDate_whenAllRangeSelected() {
        val later = Weight(id = 1, weight = 141.0, createdTime = LocalDate.now().toEpochMillis())
        val earlier = Weight(
            id = 2,
            weight = 142.0,
            createdTime = LocalDate.now().minusMonths(1).toEpochMillis(),
        )
        val state = WeightState(
            allWeights = listOf(later, earlier),
            selectedIndex = 1,
        )

        val trendWeights = state.toUiState().trendChartCardUiState.weightList

        assertEquals(listOf(earlier, later), trendWeights)
    }

    private fun LocalDate.toEpochMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
