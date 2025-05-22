package com.example.sporthub

import android.app.Application
import android.util.Log
import com.example.sporthub.utils.ThemeManager

class SportHubApplication : Application() {
    companion object {
        // Add variable to track if registration is in progress
        var isRegistrationInProgress = false
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize and apply theme

        try {
            val resources = resources
            val resourcesImpl = resources.javaClass.getDeclaredField("mResourcesImpl").apply {
                isAccessible = true
            }.get(resources)

            val cacheField = resourcesImpl.javaClass.getDeclaredField("mDrawableCache").apply {
                isAccessible = true
            }
            val drawableCache = cacheField.get(resourcesImpl)

            // Increase cache size
            val maxCacheSizeField = drawableCache.javaClass.getDeclaredField("mMaxCacheSize").apply {
                isAccessible = true
            }
            maxCacheSizeField.setInt(drawableCache, 50) // Increase cache size
        } catch (e: Exception) {
            Log.e("SportHubApplication", "Resource caching error: ${e.message}")
            // Non-critical, can continue if this fails
        }


        val themeManager = ThemeManager.getInstance(this)
        themeManager.applyTheme()

        // Initialize other utilities here if needed
    }
}