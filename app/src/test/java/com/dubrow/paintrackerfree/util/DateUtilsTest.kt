package com.dubrow.paintrackerfree.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DateUtilsTest {

    // Fixed epoch: 2024-03-15 14:30:00 UTC
    // We derive the expected strings using the same SimpleDateFormat logic so the test
    // is timezone-agnostic (it matches whatever the host JVM timezone is).
    private val knownEpoch: Long = run {
        val cal = Calendar.getInstance()
        cal.set(2024, Calendar.MARCH, 15, 14, 30, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    // --- formatCsv ---

    @Test
    fun formatCsv_knownTimestamp_matchesExpectedPattern() {
        val result = DateUtils.formatCsv(knownEpoch)
        // Must match yyyy-MM-dd HH:mm:ss
        assertTrue(
            "Expected CSV format 'yyyy-MM-dd HH:mm:ss', got: $result",
            result.matches(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}"""))
        )
    }

    @Test
    fun formatCsv_roundTrip_parsesBackToSameTimestamp() {
        val formatted = DateUtils.formatCsv(knownEpoch)
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val parsed = fmt.parse(formatted)!!.time
        assertEquals(knownEpoch, parsed)
    }

    // --- formatChartDate ---

    @Test
    fun formatChartDate_knownTimestamp_matchesSlashPattern() {
        val result = DateUtils.formatChartDate(knownEpoch)
        assertTrue(
            "Expected chart format 'M/d', got: $result",
            result.matches(Regex("""\d{1,2}/\d{1,2}"""))
        )
    }

    // --- startOfDay / endOfDay ---

    @Test
    fun startOfDay_returnsZeroTimeComponents() {
        val start = DateUtils.startOfDay(knownEpoch)
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun endOfDay_returnsEndOfDayComponents() {
        val end = DateUtils.endOfDay(knownEpoch)
        val cal = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
        assertEquals(59, cal.get(Calendar.SECOND))
        assertEquals(999, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun startOfDay_andEndOfDay_sameDateComponents() {
        val start = DateUtils.startOfDay(knownEpoch)
        val end = DateUtils.endOfDay(knownEpoch)
        val calStart = Calendar.getInstance().apply { timeInMillis = start }
        val calEnd = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(calStart.get(Calendar.YEAR), calEnd.get(Calendar.YEAR))
        assertEquals(calStart.get(Calendar.DAY_OF_YEAR), calEnd.get(Calendar.DAY_OF_YEAR))
        assertTrue("endOfDay must be after startOfDay", end > start)
    }

    @Test
    fun startOfDay_noArg_returnsStartOfToday() {
        val start = DateUtils.startOfDay()
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        val today = Calendar.getInstance()
        assertEquals(today.get(Calendar.YEAR), cal.get(Calendar.YEAR))
        assertEquals(today.get(Calendar.DAY_OF_YEAR), cal.get(Calendar.DAY_OF_YEAR))
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
    }

    // --- daysAgoMs ---

    @Test
    fun daysAgoMs_zero_returnsStartOfToday() {
        val result = DateUtils.daysAgoMs(0)
        val resultCal = Calendar.getInstance().apply { timeInMillis = result }
        val today = Calendar.getInstance()
        assertEquals(today.get(Calendar.YEAR), resultCal.get(Calendar.YEAR))
        assertEquals(today.get(Calendar.DAY_OF_YEAR), resultCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
    }

    @Test
    fun daysAgoMs_seven_returnsSevenDaysBeforeToday() {
        val sevenDaysAgo = DateUtils.daysAgoMs(7)
        val expected = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val result = Calendar.getInstance().apply { timeInMillis = sevenDaysAgo }
        assertEquals(expected.get(Calendar.YEAR), result.get(Calendar.YEAR))
        assertEquals(expected.get(Calendar.DAY_OF_YEAR), result.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun daysAgoMs_result_isAlwaysStartOfDay() {
        val result = DateUtils.daysAgoMs(30)
        val cal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
    }

    // --- toDateHeader ---

    @Test
    fun toDateHeader_today_returnsToday() {
        val now = System.currentTimeMillis()
        assertEquals("Today", DateUtils.toDateHeader(now))
    }

    @Test
    fun toDateHeader_yesterday_returnsYesterday() {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis
        assertEquals("Yesterday", DateUtils.toDateHeader(yesterday))
    }

    @Test
    fun toDateHeader_olderDate_returnsDayOfWeekFormat() {
        val older = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -5)
        }.timeInMillis
        val result = DateUtils.toDateHeader(older)
        assertFalse("Should not be 'Today'", result == "Today")
        assertFalse("Should not be 'Yesterday'", result == "Yesterday")
        // Should be something like "Sunday, March 10"
        assertTrue("Expected day-of-week format, got: $result", result.contains(","))
    }
}
