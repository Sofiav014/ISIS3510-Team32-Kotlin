package com.example.sporthub.utils

import android.content.Context
import android.content.SharedPreferences

object ImageUrlStore {
    private const val PREF_NAME = "venue_image_urls"
    private const val KEY_PREFIX = "venue_image_url_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveImageUrl(context: Context, venueId: String, url: String) {
        getPrefs(context).edit().putString(KEY_PREFIX + venueId, url).apply()
    }

    fun getImageUrl(context: Context, venueId: String): String {
        return getPrefs(context).getString(KEY_PREFIX + venueId, "") ?: ""
    }
}