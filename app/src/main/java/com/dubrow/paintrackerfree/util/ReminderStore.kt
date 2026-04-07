package com.dubrow.paintrackerfree.util

import android.content.Context
import androidx.core.content.edit

object ReminderStore {

    private const val PREFS = "reminder_prefs"
    private const val KEY_TIMES = "reminder_times"

    /** Returns saved reminder times as "HH:mm" strings, sorted ascending. */
    fun getTimes(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_TIMES, "") ?: ""
        return if (raw.isBlank()) emptyList()
        else raw.split("||").filter { it.isNotBlank() }.sorted()
    }

    fun addTime(context: Context, hhmm: String) {
        val current = getTimes(context).toMutableList()
        if (hhmm !in current) {
            current.add(hhmm)
            prefs(context).edit { putString(KEY_TIMES, current.joinToString("||")) }
        }
    }

    fun removeTime(context: Context, hhmm: String) {
        val current = getTimes(context).toMutableList()
        current.remove(hhmm)
        prefs(context).edit { putString(KEY_TIMES, current.joinToString("||")) }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
