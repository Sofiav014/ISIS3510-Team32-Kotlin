package com.example.sporthub.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.sporthub.ui.MainActivity

/**
 * Utility class for managing theme transitions without activity recreation
 */
object ThemeSwitcher {
    private const val TAG = "ThemeSwitcher"
    private const val THEME_PREFS = "theme_prefs"
    private const val KEY_THEME_CHANGING = "is_theme_changing"
    private const val KEY_IN_TRANSITION = "in_theme_transition"

    /**
     * Apply theme change with minimal activity recreation
     */
    fun applyTheme(context: Context, isDarkMode: Boolean, userId: String?) {
        Log.d(TAG, "Applying theme change: isDarkMode=$isDarkMode")

        // 1. Mark that we're in a theme transition
        markTransitionStarted(context)

        // 2. Save user preference in background if userId available
        if (userId != null) {
            Thread {
                try {
                    LocalThemeManager.saveUserTheme(context, userId, isDarkMode)
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving theme preference: ${e.message}")
                }
            }.start()
        }

        // 3. Apply theme change but delay just enough to let UI updates happen first
        Handler(Looper.getMainLooper()).postDelayed({
            val mode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }, 50)

        // 4. Clear transition flag after a slightly longer delay
        Handler(Looper.getMainLooper()).postDelayed({
            markTransitionEnded(context)
        }, 300)
    }

    /**
     * Check if we're currently in a theme transition
     */
    fun isInThemeTransition(context: Context): Boolean {
        return context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_IN_TRANSITION, false)
    }

    private fun markTransitionStarted(context: Context) {
        context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IN_TRANSITION, true)
            .putBoolean(KEY_THEME_CHANGING, true)
            .apply()
    }

    private fun markTransitionEnded(context: Context) {
        context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IN_TRANSITION, false)
            .putBoolean(KEY_THEME_CHANGING, false)
            .apply()
    }
}