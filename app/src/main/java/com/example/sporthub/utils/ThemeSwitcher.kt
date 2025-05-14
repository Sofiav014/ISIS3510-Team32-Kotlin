package com.example.sporthub.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate


object ThemeSwitcher {
    private const val PREFS_NAME = "theme_switcher_prefs"
    private const val KEY_DARK_MODE = "is_dark_mode"
    private const val TAG = "ThemeSwitcher"


    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)
            applyTheme(isDarkMode)
            Log.d(TAG, "Theme initialized: isDarkMode=$isDarkMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing theme: ${e.message}")
        }
    }


    fun toggleTheme(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentMode = prefs.getBoolean(KEY_DARK_MODE, false)
            val newMode = !currentMode

            // Save the new mode immediately
            prefs.edit().putBoolean(KEY_DARK_MODE, newMode).apply()

            // Apply the theme change
            applyTheme(newMode)

            Log.d(TAG, "Theme toggled: isDarkMode=$newMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling theme: ${e.message}")
        }
    }


    private fun applyTheme(isDarkMode: Boolean) {
        val mode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        // Apply theme on main thread to avoid potential issues
        Handler(Looper.getMainLooper()).post {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }


    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }
}