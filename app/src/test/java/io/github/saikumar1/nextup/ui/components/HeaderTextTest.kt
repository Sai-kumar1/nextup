package io.github.saikumar1.nextup.ui.components

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class HeaderTextTest {
    // Sunday, 15 November 2026.
    private val today = LocalDate.of(2026, 11, 15)
    private lateinit var savedLocale: Locale

    @Before
    fun setUp() {
        savedLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    @After
    fun tearDown() = Locale.setDefault(savedLocale)

    private fun title(offset: Long) = headerTitle(today.plusDays(offset), today)

    @Test
    fun title_isRelativeNearToday() {
        assertEquals("Today", title(0))
        assertEquals("Tomorrow", title(1))
        assertEquals("Yesterday", title(-1))
    }

    @Test
    fun title_usesWeekdayWithinAWeek() {
        assertEquals("Wednesday", title(3))
        assertEquals("Saturday", title(6))
        assertEquals("Last Thursday", title(-3))
    }

    @Test
    fun title_usesShortDateFurtherAway() {
        assertEquals("Mon, 23 Nov", title(8))
        assertEquals("Fri, 1 Jan 2027", headerTitle(LocalDate.of(2027, 1, 1), today))
    }

    @Test
    fun subtitle_showsProgress() {
        assertEquals("Nothing planned", headerSubtitle(today, today, total = 0, done = 0))
        assertEquals("3 of 5 done", headerSubtitle(today, today, total = 5, done = 3))
        assertEquals("All done ✓", headerSubtitle(today, today, total = 2, done = 2))
    }

    @Test
    fun subtitle_addsDistanceAwayFromToday() {
        assertEquals("1 task", headerSubtitle(today.plusDays(1), today, total = 1, done = 0))
        assertEquals("In 9 days · 2 tasks", headerSubtitle(today.plusDays(9), today, total = 2, done = 0))
        assertEquals("3 days ago · 1 of 4 done", headerSubtitle(today.minusDays(3), today, total = 4, done = 1))
        assertEquals("In 2 days · Nothing planned", headerSubtitle(today.plusDays(2), today, total = 0, done = 0))
    }
}
