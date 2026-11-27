package app.tinks.tink.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateTest {

    @Test
    fun haircutReminder_showsOnlyAtThirtyFiveDaysOrMore() {
        assertFalse(HomeSnapshot(haircutDays = null).showHaircutReminder)
        assertFalse(HomeSnapshot(haircutDays = 34).showHaircutReminder)
        assertTrue(HomeSnapshot(haircutDays = 35).showHaircutReminder)
        assertTrue(HomeSnapshot(haircutDays = 42).showHaircutReminder)
    }
}
