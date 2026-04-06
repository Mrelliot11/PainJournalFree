package com.example.paintrackerfree.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private var dateTimeFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    private var timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private var dateHeaderFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    private var chartDateFormat = SimpleDateFormat("M/d", Locale.getDefault())
    private var chartDateYearFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
    private var csvDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun formatDateTime(epochMs: Long): String = dateTimeFormat.format(Date(epochMs))
    fun formatTime(epochMs: Long): String = timeFormat.format(Date(epochMs))
    fun formatChartDate(epochMs: Long): String = chartDateFormat.format(Date(epochMs))
    fun formatChartDateYear(epochMs: Long): String = chartDateYearFormat.format(Date(epochMs))
    fun formatCsv(epochMs: Long): String = csvDateFormat.format(Date(epochMs))

    fun toDateHeader(epochMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(cal, today) -> "Today"
            isSameDay(cal, yesterday) -> "Yesterday"
            else -> dateHeaderFormat.format(Date(epochMs))
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    fun startOfDay(epochMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDay(epochMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun daysAgoMs(days: Int): Long {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
        return startOfDay(cal.timeInMillis)
    }
}
