// app/src/main/java/com/example/sporthub/data/db/FavoriteVenueDao.kt
package com.example.sporthub.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for favorite venues
 * NOTE: No suspend functions to avoid kapt processing issues
 */
@Dao
interface FavoriteVenueDao {
    /**
     * Get stream of favorite venues for a user
     */
    @Query("SELECT * FROM favorite_venues WHERE userId = :userId AND isFavorite = 1")
    fun getAllFavoriteVenues(userId: String): Flow<List<VenueEntity>>

    /**
     * Insert a single venue
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVenue(venue: VenueEntity): Long

    /**
     * Insert multiple venues at once
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVenues(venues: List<VenueEntity>): List<Long>

    /**
     * Mark a venue as not favorite (soft delete)
     */
    @Query("UPDATE favorite_venues SET isFavorite = 0 WHERE id = :venueId AND userId = :userId")
    fun removeFromFavorites(venueId: String, userId: String): Int

    /**
     * Get a specific favorite venue
     */
    @Query("SELECT * FROM favorite_venues WHERE id = :venueId AND userId = :userId AND isFavorite = 1 LIMIT 1")
    fun getFavoriteVenue(venueId: String, userId: String): VenueEntity?

    /**
     * Get venues updated after a timestamp
     */
    @Query("SELECT * FROM favorite_venues WHERE lastSyncedTimestamp > :timestamp AND userId = :userId")
    fun getVenuesUpdatedAfter(timestamp: Long, userId: String): List<VenueEntity>

    /**
     * Permanently delete a venue
     */
    @Query("DELETE FROM favorite_venues WHERE id = :venueId AND userId = :userId")
    fun deleteVenue(venueId: String, userId: String): Int
}