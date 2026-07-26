package app.tinks.tink.home

import app.tinks.tink.book.BookPageFormat
import org.junit.Assert.assertEquals
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
    fun readKeeperBook_usesPageFormatForProgressInputLabel() {
        val pages = readKeeperBook(BookPageFormat.Page)
        val percent = readKeeperBook(BookPageFormat.Percent100)

        assertEquals("Page", pages.progressTargetLabel)
        assertEquals("Progress %", percent.progressTargetLabel)
    }

    private fun readKeeperBook(pageFormat: BookPageFormat) = HomeReadKeeperBook(
        id = 1,
        title = "The Left Hand of Darkness",
        coverUrl = null,
        pageFormat = pageFormat,
        currentPage = 120,
        progressPercentage = null,
        pages = 360,
    )
}
