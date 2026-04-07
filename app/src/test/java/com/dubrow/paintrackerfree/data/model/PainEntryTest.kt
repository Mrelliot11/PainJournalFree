package com.dubrow.paintrackerfree.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PainEntryTest {

    // --- Default values ---

    @Test
    fun constructor_defaultId_isZero() {
        val entry = PainEntry(painLevel = 5)
        assertEquals(0L, entry.id)
    }

    @Test
    fun constructor_defaultStringFields_areEmpty() {
        val entry = PainEntry(painLevel = 5)
        assertEquals("", entry.locations)
        assertEquals("", entry.painTypes)
        assertEquals("", entry.triggers)
        assertEquals("", entry.medications)
        assertEquals("", entry.notes)
    }

    @Test
    fun constructor_defaultMood_isThree() {
        val entry = PainEntry(painLevel = 5)
        assertEquals(3, entry.mood)
    }

    @Test
    fun constructor_defaultSleepQuality_isThree() {
        val entry = PainEntry(painLevel = 5)
        assertEquals(3, entry.sleepQuality)
    }

    @Test
    fun constructor_defaultTimestamp_isRecent() {
        val before = System.currentTimeMillis()
        val entry = PainEntry(painLevel = 5)
        val after = System.currentTimeMillis()
        assertTrue(entry.timestamp in before..after)
    }

    // --- Explicit field assignment ---

    @Test
    fun constructor_allFieldsSet_retainedCorrectly() {
        val entry = PainEntry(
            id = 42L,
            timestamp = 1_000_000L,
            painLevel = 7,
            locations = "Back, Neck",
            painTypes = "Burning",
            triggers = "Stress",
            medications = "Ibuprofen",
            mood = 2,
            sleepQuality = 4,
            notes = "Rough day"
        )
        assertEquals(42L, entry.id)
        assertEquals(1_000_000L, entry.timestamp)
        assertEquals(7, entry.painLevel)
        assertEquals("Back, Neck", entry.locations)
        assertEquals("Burning", entry.painTypes)
        assertEquals("Stress", entry.triggers)
        assertEquals("Ibuprofen", entry.medications)
        assertEquals(2, entry.mood)
        assertEquals(4, entry.sleepQuality)
        assertEquals("Rough day", entry.notes)
    }

    // --- Boundary values for painLevel ---

    @Test
    fun painLevel_zero_isValid() {
        val entry = PainEntry(painLevel = 0)
        assertEquals(0, entry.painLevel)
    }

    @Test
    fun painLevel_ten_isValid() {
        val entry = PainEntry(painLevel = 10)
        assertEquals(10, entry.painLevel)
    }

    // --- data class copy ---

    @Test
    fun copy_changedPainLevel_otherFieldsUnchanged() {
        val original = PainEntry(
            painLevel = 3,
            locations = "Head",
            notes = "Mild"
        )
        val updated = original.copy(painLevel = 8)
        assertEquals(8, updated.painLevel)
        assertEquals("Head", updated.locations)
        assertEquals("Mild", updated.notes)
        assertEquals(original.timestamp, updated.timestamp)
    }

    @Test
    fun copy_newId_originalIdUnchanged() {
        val original = PainEntry(painLevel = 5)
        val withId = original.copy(id = 99L)
        assertEquals(0L, original.id)
        assertEquals(99L, withId.id)
    }

    // --- Equality ---

    @Test
    fun equals_sameFields_areEqual() {
        val ts = 123456789L
        val a = PainEntry(id = 1, timestamp = ts, painLevel = 5)
        val b = PainEntry(id = 1, timestamp = ts, painLevel = 5)
        assertEquals(a, b)
    }

    @Test
    fun equals_differentPainLevel_areNotEqual() {
        val ts = 123456789L
        val a = PainEntry(id = 1, timestamp = ts, painLevel = 3)
        val b = PainEntry(id = 1, timestamp = ts, painLevel = 7)
        assertNotEquals(a, b)
    }


}
