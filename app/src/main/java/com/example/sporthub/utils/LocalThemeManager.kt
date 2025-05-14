package com.example.sporthub.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate

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
            // Check connectivity
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val isOffline = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                connectivityManager.activeNetworkInfo?.isConnected != true
            }

            // If offline, use the offline method
            if (isOffline) {
                Log.d(TAG, "Network offline, using offline save method")
                saveUserThemeOffline(context, userId, isDarkMode)
                return
            }

            // Continue with normal online saving
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

    fun applyUserTheme(context: Context, userId: String) {
        try {
            val isDarkMode = getUserTheme(context, userId)

            if (isDarkMode != null) {
                // Apply theme only if it's different from current to avoid unnecessary recreation
                val currentMode = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) true else false

                if (isDarkMode != currentMode) {
                    AppCompatDelegate.setDefaultNightMode(
                        if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("LocalThemeManager", "Error applying theme: ${e.message}")
        }
    }

    fun saveUserThemeOffline(context: Context, userId: String, isDarkMode: Boolean) {
        try {
            val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putBoolean(KEY_PREFIX + userId, isDarkMode)
                .apply()
            Log.d(TAG, "Saved offline theme preference for user $userId: isDarkMode=$isDarkMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving offline theme preference: ${e.message}")
        }
    }
}