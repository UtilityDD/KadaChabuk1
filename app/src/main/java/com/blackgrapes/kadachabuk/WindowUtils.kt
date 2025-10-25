package com.blackgrapes.kadachabuk

import android.content.res.Configuration
import android.view.Window
import androidx.core.view.ViewCompat

/**
 * A utility object for handling window-related configurations, such as status bar icon colors.
 */
object WindowUtils {

    /**
     * Sets the system status bar icon colors based on the current theme.
     * This enforces the app's specific style: black icons in dark mode, and white icons in light mode.
     *
     * @param window The window whose status bar icons need to be controlled.
     */
    fun setStatusBarIconColor(window: Window) {
        ViewCompat.getWindowInsetsController(window.decorView)?.let { controller ->
            // Set isAppearanceLightStatusBars to false to always enforce light (e.g., white) icons.
            controller.isAppearanceLightStatusBars = false
        }
    }
}