package com.example.paintrackerfree.util

import android.content.Context
import androidx.core.content.edit

object BehaviourStore {

    private const val PREFS = "behaviour_prefs"
    private const val KEY_SWIPE_TO_DELETE = "swipe_to_delete"

    fun isSwipeToDeleteEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SWIPE_TO_DELETE, true)

    fun setSwipeToDeleteEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_SWIPE_TO_DELETE, enabled) }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
