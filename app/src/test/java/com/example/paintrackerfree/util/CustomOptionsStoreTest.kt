package com.example.paintrackerfree.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomOptionsStoreTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("custom_options", 0).edit().clear().commit()
    }

    // --- Locations ---

    @Test
    fun locations_defaultIsEmpty() {
        assertTrue(CustomOptionsStore.getLocations(context).isEmpty())
    }

    @Test
    fun addLocation_appearsInList() {
        CustomOptionsStore.addLocation(context, "Back")
        assertTrue(CustomOptionsStore.getLocations(context).contains("Back"))
    }

    @Test
    fun addLocation_duplicate_notAddedTwice() {
        CustomOptionsStore.addLocation(context, "Back")
        CustomOptionsStore.addLocation(context, "Back")
        assertEquals(1, CustomOptionsStore.getLocations(context).size)
    }

    @Test
    fun addLocation_multiple_allPresent() {
        CustomOptionsStore.addLocation(context, "Back")
        CustomOptionsStore.addLocation(context, "Neck")
        CustomOptionsStore.addLocation(context, "Head")
        val locations = CustomOptionsStore.getLocations(context)
        assertTrue(locations.contains("Back"))
        assertTrue(locations.contains("Neck"))
        assertTrue(locations.contains("Head"))
    }

    @Test
    fun removeLocation_removesCorrectEntry() {
        CustomOptionsStore.addLocation(context, "Back")
        CustomOptionsStore.addLocation(context, "Neck")
        CustomOptionsStore.removeLocation(context, "Back")
        val locations = CustomOptionsStore.getLocations(context)
        assertFalse(locations.contains("Back"))
        assertTrue(locations.contains("Neck"))
    }

    @Test
    fun removeLocation_nonExistent_noError() {
        CustomOptionsStore.addLocation(context, "Back")
        CustomOptionsStore.removeLocation(context, "Shoulder") // doesn't exist
        assertEquals(1, CustomOptionsStore.getLocations(context).size)
    }

    @Test
    fun removeLocation_last_resultsInEmptyList() {
        CustomOptionsStore.addLocation(context, "Back")
        CustomOptionsStore.removeLocation(context, "Back")
        assertTrue(CustomOptionsStore.getLocations(context).isEmpty())
    }

    // --- Pain types ---

    @Test
    fun painTypes_defaultIsEmpty() {
        assertTrue(CustomOptionsStore.getPainTypes(context).isEmpty())
    }

    @Test
    fun addPainType_appearsInList() {
        CustomOptionsStore.addPainType(context, "Burning")
        assertTrue(CustomOptionsStore.getPainTypes(context).contains("Burning"))
    }

    @Test
    fun removePainType_removesEntry() {
        CustomOptionsStore.addPainType(context, "Burning")
        CustomOptionsStore.removePainType(context, "Burning")
        assertTrue(CustomOptionsStore.getPainTypes(context).isEmpty())
    }

    // --- Triggers ---

    @Test
    fun triggers_defaultIsEmpty() {
        assertTrue(CustomOptionsStore.getTriggers(context).isEmpty())
    }

    @Test
    fun addTrigger_appearsInList() {
        CustomOptionsStore.addTrigger(context, "Stress")
        assertTrue(CustomOptionsStore.getTriggers(context).contains("Stress"))
    }

    @Test
    fun removeTrigger_removesEntry() {
        CustomOptionsStore.addTrigger(context, "Stress")
        CustomOptionsStore.removeTrigger(context, "Stress")
        assertTrue(CustomOptionsStore.getTriggers(context).isEmpty())
    }

    // --- Isolation between categories ---

    @Test
    fun locations_painTypes_triggers_areIndependent() {
        CustomOptionsStore.addLocation(context, "Back")
        CustomOptionsStore.addPainType(context, "Burning")
        CustomOptionsStore.addTrigger(context, "Stress")

        assertFalse(CustomOptionsStore.getLocations(context).contains("Burning"))
        assertFalse(CustomOptionsStore.getPainTypes(context).contains("Stress"))
        assertFalse(CustomOptionsStore.getTriggers(context).contains("Back"))
    }
}
