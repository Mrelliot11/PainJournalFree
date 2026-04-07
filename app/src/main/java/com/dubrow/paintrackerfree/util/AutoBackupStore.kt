package com.dubrow.paintrackerfree.util

import android.content.Context
import androidx.core.content.edit

object AutoBackupStore {

    private const val PREFS = "auto_backup_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_FREQUENCY = "frequency"

    const val FREQUENCY_DAILY = "daily"
    const val FREQUENCY_WEEKLY = "weekly"
    const val FREQUENCY_MONTHLY = "monthly"

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }

    fun getFrequency(context: Context): String =
        prefs(context).getString(KEY_FREQUENCY, FREQUENCY_WEEKLY) ?: FREQUENCY_WEEKLY

    fun setFrequency(context: Context, frequency: String) =
        prefs(context).edit { putString(KEY_FREQUENCY, frequency) }

    fun getFolderUri(context: Context): String? =
        prefs(context).getString("folder_uri", null)

    fun setFolderUri(context: Context, uri: String?) =
        prefs(context).edit { putString("folder_uri", uri) }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
