package com.example.sporthub.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

/**
 * Manager class for handling theme-related functionality
 */
class ThemeManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"

        const val MODE_LIGHT = 0
        const val MODE_DARK = 1
        const val MODE_SYSTEM = 2

        // Singleton instance
        @Volatile
        private var INSTANCE: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Gets the current theme mode from SharedPreferences
     */
    fun getThemeMode(): Int {
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM)
    }

    /**
     * Sets the theme mode in SharedPreferences and applies it
     */
    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        applyTheme(mode)
    }

    /**
     * Apply the theme based on the mode
     */
    fun applyTheme(mode: Int = getThemeMode()) {
        when (mode) {
            MODE_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            MODE_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            MODE_SYSTEM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
        }
    }

    /**
     * Checks if dark mode is currently active
     */
    fun isDarkModeActive(): Boolean {
        return when (getThemeMode()) {
            MODE_LIGHT -> false
            MODE_DARK -> true
            else -> AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        }
    }

    /**
     * Toggle between light and dark modes
     */
    fun toggleDarkMode() {
        val currentMode = getThemeMode()
        if (currentMode == MODE_DARK) {
            setThemeMode(MODE_LIGHT)
        } else {
            setThemeMode(MODE_DARK)
        }
    }
}