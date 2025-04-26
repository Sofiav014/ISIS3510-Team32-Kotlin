package com.example.sporthub.utils

import android.content.Context
import android.util.Log

object LocalThemeManager {

    private const val PREF_NAME = "user_theme_prefs"
    private const val KEY_PREFIX = "theme_for_user_"
    private const val TAG = "LocalThemeManager"

    /**
     * Save the user's theme preference
     *
     * @param context Application context
     * @param userId The user ID to associate with this theme preference
     * @param isDarkMode True if the user prefers dark mode, false for light mode
     */
    fun saveUserTheme(context: Context, userId: String, isDarkMode: Boolean) {
        try {
            val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putBoolean(KEY_PREFIX + userId, isDarkMode)
                .apply()
            Log.d(TAG, "Saved theme preference for user $userId: isDarkMode=$isDarkMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving theme preference: ${e.message}")
        }
    }

    /**
     * Get the user's theme preference
     *
     * @param context Application context
     * @param userId The user ID to look up
     * @return Boolean? - true for dark mode, false for light mode, null if no preference set
     */
    fun getUserTheme(context: Context, userId: String): Boolean? {
        try {
            val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return if (sharedPrefs.contains(KEY_PREFIX + userId)) {
                val isDarkMode = sharedPrefs.getBoolean(KEY_PREFIX + userId, false)
                Log.d(TAG, "Retrieved theme preference for user $userId: isDarkMode=$isDarkMode")
                isDarkMode
            } else {
                Log.d(TAG, "No theme preference found for user $userId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving theme preference: ${e.message}")
            return null
        }
    }

    /**
     * Clear the user's theme preference
     */
    fun clearUserTheme(context: Context, userId: String) {
        try {
            val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .remove(KEY_PREFIX + userId)
                .apply()
            Log.d(TAG, "Cleared theme preference for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing theme preference: ${e.message}")
        }
    }

    /**
     * Clear all theme preferences (useful for debugging)
     */
    fun clearAllThemePreferences(context: Context) {
        try {
            val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sharedPrefs.edit().clear().apply()
            Log.d(TAG, "Cleared all theme preferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all theme preferences: ${e.message}")
        }
    }
}