package com.example.paintrackerfree.util

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeStoreTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("theme_prefs", 0).edit().clear().commit()
    }

    @Test
    fun getMode_defaultIsFollowSystem() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeStore.getMode(context))
    }

    @Test
    fun setMode_light_persists() {
        ThemeStore.setMode(context, AppCompatDelegate.MODE_NIGHT_NO)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeStore.getMode(context))
    }

    @Test
    fun setMode_dark_persists() {
        ThemeStore.setMode(context, AppCompatDelegate.MODE_NIGHT_YES)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeStore.getMode(context))
    }

    @Test
    fun setMode_followSystem_persists() {
        ThemeStore.setMode(context, AppCompatDelegate.MODE_NIGHT_YES)
        ThemeStore.setMode(context, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeStore.getMode(context))
    }

    @Test
    fun applyStored_doesNotThrow() {
        ThemeStore.setMode(context, AppCompatDelegate.MODE_NIGHT_NO)
        // Just verify it doesn't crash — actual UI application requires a running Activity
        ThemeStore.applyStored(context)
    }
}
