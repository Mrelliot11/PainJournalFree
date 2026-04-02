package com.example.paintrackerfree.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

object ThemeStore {

    private const val PREFS = "theme_prefs"
    private const val KEY_MODE = "night_mode"

    // Matches AppCompatDelegate MODE_NIGHT_* constants:
    // -1 = follow system, 1 = light, 2 = dark
    fun getMode(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun setMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putInt(KEY_MODE, mode) }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun applyStored(context: Context) {
        AppCompatDelegate.setDefaultNightMode(getMode(context))
    }
}
