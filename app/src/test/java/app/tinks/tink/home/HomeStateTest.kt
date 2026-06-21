package app.tinks.tink.home

import app.tinks.tink.book.BookPageFormat
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

    @Test
    fun readKeeperBook_reportsActiveSessionFromCachedStartTime() {
        val inactive = readKeeperBook(sessionStartedAt = null)
        val active = readKeeperBook(sessionStartedAt = 1779177600000L)

        assertFalse(inactive.hasActiveSession)
        assertTrue(active.hasActiveSession)
    }

    private fun readKeeperBook(sessionStartedAt: Long?) = HomeReadKeeperBook(
        id = 1,
        title = "The Left Hand of Darkness",
        coverUrl = null,
        pageFormat = BookPageFormat.Page,
        currentPage = 120,
        progressPercentage = null,
        pages = 360,
        sessionStartedAt = sessionStartedAt,
        sessionStartPage = null,
        sessionStartProgressPercentage = null,
    )
}
