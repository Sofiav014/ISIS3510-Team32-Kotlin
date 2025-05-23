// app/src/main/java/com/example/sporthub/data/db/Converters.kt
package com.example.sporthub.data.db

import androidx.room.TypeConverter
import java.util.Date

/**
 * Minimal type converters for Room database
 * Only including converters needed for our entity
 */
class Converters {
    /**
     * Convert Long timestamp to Date
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Convert Date to Long timestamp
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    /**
     * Convert Boolean to Int for better SQLite compatibility
     */
    @TypeConverter
    fun fromBoolean(value: Boolean): Int {
        return if (value) 1 else 0
    }

    /**
     * Convert Int to Boolean for better SQLite compatibility
     */
    @TypeConverter
    fun toBoolean(value: Int): Boolean {
        return value == 1
    }
}