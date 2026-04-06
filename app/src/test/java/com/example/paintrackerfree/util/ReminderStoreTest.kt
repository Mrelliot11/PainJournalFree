package com.example.paintrackerfree.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderStoreTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("reminder_prefs", 0).edit().clear().commit()
    }

    @Test
    fun times_defaultIsEmpty() {
        assertTrue(ReminderStore.getTimes(context).isEmpty())
    }

    @Test
    fun addTime_appearsInList() {
        ReminderStore.addTime(context, "09:00")
        assertTrue(ReminderStore.getTimes(context).contains("09:00"))
    }

    @Test
    fun addTime_duplicate_notAddedTwice() {
        ReminderStore.addTime(context, "09:00")
        ReminderStore.addTime(context, "09:00")
        assertEquals(1, ReminderStore.getTimes(context).size)
    }

    @Test
    fun addTime_multiple_allPresent() {
        ReminderStore.addTime(context, "08:00")
        ReminderStore.addTime(context, "12:00")
        ReminderStore.addTime(context, "20:00")
        val times = ReminderStore.getTimes(context)
        assertEquals(3, times.size)
        assertTrue(times.contains("08:00"))
        assertTrue(times.contains("12:00"))
        assertTrue(times.contains("20:00"))
    }

    @Test
    fun getTimes_returnsSortedAscending() {
        ReminderStore.addTime(context, "20:00")
        ReminderStore.addTime(context, "08:00")
        ReminderStore.addTime(context, "12:00")
        val times = ReminderStore.getTimes(context)
        assertEquals(listOf("08:00", "12:00", "20:00"), times)
    }

    @Test
    fun removeTime_removesCorrectEntry() {
        ReminderStore.addTime(context, "08:00")
        ReminderStore.addTime(context, "12:00")
        ReminderStore.removeTime(context, "08:00")
        val times = ReminderStore.getTimes(context)
        assertFalse(times.contains("08:00"))
        assertTrue(times.contains("12:00"))
    }

    @Test
    fun removeTime_nonExistent_noError() {
        ReminderStore.addTime(context, "08:00")
        ReminderStore.removeTime(context, "20:00") // doesn't exist
        assertEquals(1, ReminderStore.getTimes(context).size)
    }

    @Test
    fun removeTime_last_resultsInEmptyList() {
        ReminderStore.addTime(context, "09:00")
        ReminderStore.removeTime(context, "09:00")
        assertTrue(ReminderStore.getTimes(context).isEmpty())
    }
}
