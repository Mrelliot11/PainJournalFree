package com.example.paintrackerfree.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoBackupStoreTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("auto_backup_prefs", 0).edit().clear().commit()
    }

    // --- enabled ---

    @Test
    fun enabled_defaultIsFalse() {
        assertFalse(AutoBackupStore.isEnabled(context))
    }

    @Test
    fun setEnabled_true_persists() {
        AutoBackupStore.setEnabled(context, true)
        assertTrue(AutoBackupStore.isEnabled(context))
    }

    @Test
    fun setEnabled_false_persists() {
        AutoBackupStore.setEnabled(context, true)
        AutoBackupStore.setEnabled(context, false)
        assertFalse(AutoBackupStore.isEnabled(context))
    }

    // --- frequency ---

    @Test
    fun frequency_defaultIsWeekly() {
        assertEquals(AutoBackupStore.FREQUENCY_WEEKLY, AutoBackupStore.getFrequency(context))
    }

    @Test
    fun setFrequency_daily_persists() {
        AutoBackupStore.setFrequency(context, AutoBackupStore.FREQUENCY_DAILY)
        assertEquals(AutoBackupStore.FREQUENCY_DAILY, AutoBackupStore.getFrequency(context))
    }

    @Test
    fun setFrequency_monthly_persists() {
        AutoBackupStore.setFrequency(context, AutoBackupStore.FREQUENCY_MONTHLY)
        assertEquals(AutoBackupStore.FREQUENCY_MONTHLY, AutoBackupStore.getFrequency(context))
    }

    // --- folderUri ---

    @Test
    fun folderUri_defaultIsNull() {
        assertNull(AutoBackupStore.getFolderUri(context))
    }

    @Test
    fun setFolderUri_value_persists() {
        AutoBackupStore.setFolderUri(context, "content://some/uri")
        assertEquals("content://some/uri", AutoBackupStore.getFolderUri(context))
    }

    @Test
    fun setFolderUri_null_clearsValue() {
        AutoBackupStore.setFolderUri(context, "content://some/uri")
        AutoBackupStore.setFolderUri(context, null)
        assertNull(AutoBackupStore.getFolderUri(context))
    }
}
