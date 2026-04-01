package com.example.paintrackerfree.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pads the AppBarLayout's top by the status bar height so it clears the system notification area.
 * Applying to AppBarLayout (not the inner Toolbar) ensures the CoordinatorLayout behaviour
 * correctly reserves the full expanded height for the app bar, preventing content from sliding
 * underneath it. Works on all Android versions via WindowInsetsCompat.
 */
fun View.applyStatusBarPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        v.setPadding(
            v.paddingLeft,
            statusBar.top,
            v.paddingRight,
            v.paddingBottom
        )
        insets
    }
}
