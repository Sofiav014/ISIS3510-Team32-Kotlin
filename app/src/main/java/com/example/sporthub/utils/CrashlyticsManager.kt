package com.example.sporthub.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashlyticsManager {
    private const val TAG = "CrashlyticsManager"
    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun setCurrentScreen(screenName: String) {
        try {
            crashlytics.setCustomKey("current_screen", screenName)
            Log.d(TAG, "Set current screen to: $screenName")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting current screen: ${e.message}")
        }
    }

    fun logException(e: Throwable, message: String? = null) {
        try {
            message?.let {
                crashlytics.log("Error: $it")
            }
            crashlytics.recordException(e)
        } catch (ex: Exception) {
            Log.e(TAG, "Error logging exception: ${ex.message}")
        }
    }

    fun log(message: String) {
        try {
            crashlytics.log(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging message: ${e.message}")
        }
    }
}