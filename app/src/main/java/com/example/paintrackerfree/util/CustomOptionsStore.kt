package com.example.paintrackerfree.util

import android.content.Context
import androidx.core.content.edit

object CustomOptionsStore {

    private const val PREFS = "custom_options"
    private const val KEY_LOCATIONS = "custom_locations"
    private const val KEY_PAIN_TYPES = "custom_pain_types"
    private const val KEY_TRIGGERS = "custom_triggers"

    fun getLocations(context: Context): List<String> = load(context, KEY_LOCATIONS)
    fun getPainTypes(context: Context): List<String> = load(context, KEY_PAIN_TYPES)
    fun getTriggers(context: Context): List<String> = load(context, KEY_TRIGGERS)

    fun addLocation(context: Context, value: String) = add(context, KEY_LOCATIONS, value)
    fun addPainType(context: Context, value: String) = add(context, KEY_PAIN_TYPES, value)
    fun addTrigger(context: Context, value: String) = add(context, KEY_TRIGGERS, value)

    fun removeLocation(context: Context, value: String) = remove(context, KEY_LOCATIONS, value)
    fun removePainType(context: Context, value: String) = remove(context, KEY_PAIN_TYPES, value)
    fun removeTrigger(context: Context, value: String) = remove(context, KEY_TRIGGERS, value)

    private fun load(context: Context, key: String): List<String> {
        val raw = prefs(context).getString(key, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("||").filter { it.isNotBlank() }
    }

    private fun add(context: Context, key: String, value: String) {
        val current = load(context, key).toMutableList()
        if (value !in current) {
            current.add(value)
            prefs(context).edit { putString(key, current.joinToString("||")) }
        }
    }

    private fun remove(context: Context, key: String, value: String) {
        val current = load(context, key).toMutableList()
        current.remove(value)
        prefs(context).edit { putString(key, current.joinToString("||")) }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
