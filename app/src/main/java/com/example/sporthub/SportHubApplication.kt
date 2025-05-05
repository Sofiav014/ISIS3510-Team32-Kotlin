package com.example.sporthub

import android.app.Application
import com.example.sporthub.utils.ThemeManager

class SportHubApplication : Application() {
    companion object {
        // Add variable to track if registration is in progress
        var isRegistrationInProgress = false
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize and apply theme
        val themeManager = ThemeManager.getInstance(this)
        themeManager.applyTheme()

        // Initialize other utilities here if needed
    }
}