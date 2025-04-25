package com.example.sporthub.utils

import android.content.Context

object LocalThemeManager {

    private const val PREF_NAME = "user_theme_prefs"
    private const val KEY_PREFIX = "theme_for_user_"

    fun saveUserTheme(context: Context, userId: String, isDarkMode: Boolean) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean(KEY_PREFIX + userId, isDarkMode)
            .apply()
    }

    fun getUserTheme(context: Context, userId: String): Boolean? {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return if (sharedPrefs.contains(KEY_PREFIX + userId)) {
            sharedPrefs.getBoolean(KEY_PREFIX + userId, false) // false = light mode by default
        } else {
            null
        }
    }

    fun clearUserTheme(context: Context, userId: String) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .remove(KEY_PREFIX + userId)
            .apply()
    }
}
