package com.example.paintrackerfree.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BehaviourStoreTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before each test
        context.getSharedPreferences("behaviour_prefs", 0).edit().clear().commit()
    }

    @Test
    fun swipeToDelete_defaultIsTrue() {
        assertTrue(BehaviourStore.isSwipeToDeleteEnabled(context))
    }

    @Test
    fun setSwipeToDelete_false_persists() {
        BehaviourStore.setSwipeToDeleteEnabled(context, false)
        assertFalse(BehaviourStore.isSwipeToDeleteEnabled(context))
    }

    @Test
    fun setSwipeToDelete_true_persists() {
        BehaviourStore.setSwipeToDeleteEnabled(context, false)
        BehaviourStore.setSwipeToDeleteEnabled(context, true)
        assertTrue(BehaviourStore.isSwipeToDeleteEnabled(context))
    }
}
