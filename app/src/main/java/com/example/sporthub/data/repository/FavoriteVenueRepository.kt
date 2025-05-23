// app/src/main/java/com/example/sporthub/data/repository/FavoriteVenueRepository.kt
package com.example.sporthub.data.repository

import android.content.Context
import android.util.Log
import com.example.sporthub.data.db.FavoriteVenueDao
import com.example.sporthub.data.db.VenueEntity
import com.example.sporthub.data.model.Venue
import com.example.sporthub.utils.ConnectivityHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FavoriteVenueRepository(
    private val favoriteVenueDao: FavoriteVenueDao,
    private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "FavoriteVenueRepo"

    // Get all favorite venues for a user as a Flow
    fun getFavoriteVenues(userId: String): Flow<List<Venue>> {
        return favoriteVenueDao.getAllFavoriteVenues(userId).map { entities ->
            entities.map { it.toVenue() }
        }
    }

    // Add a venue to favorites
    suspend fun addToFavorites(venue: Venue, userId: String) {
        withContext(Dispatchers.IO) {
            // Add to local database
            val venueEntity = VenueEntity.fromVenue(venue, userId)
            favoriteVenueDao.insertVenue(venueEntity)

            // Try to update remote if we're online
            if (ConnectivityHelper.isNetworkAvailable(context)) {
                try {
                    updateRemoteFavorites(userId, venue, true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding to remote favorites: ${e.message}")
                    // Will be synced later
                }
            }
        }
    }

    // Remove venue from favorites
    suspend fun removeFromFavorites(venueId: String, userId: String) {
        withContext(Dispatchers.IO) {
            // Update local database
            favoriteVenueDao.removeFromFavorites(venueId, userId)

            // Try to update remote if we're online
            if (ConnectivityHelper.isNetworkAvailable(context)) {
                try {
                    // First get the venue details
                    val venue = favoriteVenueDao.getFavoriteVenue(venueId, userId)?.toVenue()
                    if (venue != null) {
                        updateRemoteFavorites(userId, venue, false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing from remote favorites: ${e.message}")
                    // Will be handled during next sync
                }
            }
        }
    }

    // Check if a venue is favorited
    suspend fun isVenueFavorite(venueId: String, userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            favoriteVenueDao.getFavoriteVenue(venueId, userId) != null
        }
    }

    // Sync local changes with remote
    suspend fun syncWithRemote(userId: String) {
        if (ConnectivityHelper.isNetworkAvailable(context)) {
            withContext(Dispatchers.IO) {
                // Get timestamp of last sync
                val lastSyncTime = getLastSyncTime(userId)

                // First, push local changes to remote
                val updatedVenues = favoriteVenueDao.getVenuesUpdatedAfter(lastSyncTime, userId)
                for (venueEntity in updatedVenues) {
                    val venue = venueEntity.toVenue()
                    updateRemoteFavorites(userId, venue, venueEntity.isFavorite)
                }

                // Then, fetch remote changes and update local
                fetchRemoteFavorites(userId)

                // Update last sync time
                saveLastSyncTime(userId, System.currentTimeMillis())
            }
        }
    }

    private suspend fun updateRemoteFavorites(userId: String, venue: Venue, isAdding: Boolean) {
        withContext(Dispatchers.IO) {
            val userRef = firestore.collection("users").document(userId)

            try {
                if (isAdding) {
                    // Convert venue to Map for Firestore
                    val venueMap = hashMapOf(
                        "id" to venue.id,
                        "name" to venue.name,
                        "location_name" to venue.locationName,
                        "image" to venue.image,
                        "rating" to venue.rating,
                        "sport" to venue.sport?.let {
                            hashMapOf(
                                "id" to it.id,
                                "name" to it.name,
                                "logo" to it.logo
                            )
                        }
                    )

                    // Add to venues_liked array
                    userRef.update("venues_liked", FieldValue.arrayUnion(venueMap)).await()
                } else {
                    // For removing, we need to identify the exact object in the array
                    // First get the user document
                    val userDoc = userRef.get().await()

                    if (userDoc.exists()) {
                        // Get venues_liked array
                        val venuesLiked = userDoc.get("venues_liked") as? List<Map<String, Any>> ?: listOf()

                        // Find matching venue by ID and remove it
                        val venueToRemove = venuesLiked.find { it["id"] == venue.id }
                        if (venueToRemove != null) {
                            userRef.update("venues_liked", FieldValue.arrayRemove(venueToRemove)).await()
                        } else {

                        }
                    } else {

                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating remote favorites: ${e.message}")
                throw e
            }
        }
    }

    private suspend fun fetchRemoteFavorites(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()

                if (userDoc.exists()) {
                    val venuesLikedAny = userDoc.get("venues_liked")

                    if (venuesLikedAny is List<*>) {
                        val entities = venuesLikedAny.mapNotNull { item ->
                            if (item is Map<*, *>) {
                                try {
                                    VenueEntity(
                                        id = item["id"] as? String ?: return@mapNotNull null,
                                        userId = userId,
                                        name = item["name"] as? String ?: "",
                                        locationName = item["location_name"] as? String ?: "",
                                        image = item["image"] as? String ?: "",
                                        rating = (item["rating"] as? Number)?.toDouble() ?: 0.0,
                                        sportId = (item["sport"] as? Map<*, *>)?.get("id") as? String,
                                        sportName = (item["sport"] as? Map<*, *>)?.get("name") as? String,
                                        sportLogo = (item["sport"] as? Map<*, *>)?.get("logo") as? String
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing venue: ${e.message}")
                                    null
                                }
                            } else null
                        }

                        // Update local database with remote venues
                        if (entities.isNotEmpty()) {
                            favoriteVenueDao.insertVenues(entities)
                        } else {

                        }
                    } else {

                    }
                } else {

                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote favorites: ${e.message}")
            }
        }
    }

    // Helper methods for last sync time
    private fun getLastSyncTime(userId: String): Long {
        val prefs = context.getSharedPreferences("favorite_sync_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("${userId}_last_sync", 0)
    }

    private fun saveLastSyncTime(userId: String, time: Long) {
        val prefs = context.getSharedPreferences("favorite_sync_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("${userId}_last_sync", time).apply()
    }
}