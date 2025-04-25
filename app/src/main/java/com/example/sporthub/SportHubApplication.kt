package com.example.sporthub

import android.app.Application
import com.example.sporthub.utils.ThemeManager

class SportHubApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize and apply theme
        val themeManager = ThemeManager.getInstance(this)
        themeManager.applyTheme()

        // Future app-wide initializations can go here
        // For example: crash reporting, analytics, etc.
    }
}