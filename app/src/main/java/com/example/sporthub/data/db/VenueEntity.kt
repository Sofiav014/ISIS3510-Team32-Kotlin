package com.example.sporthub.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.Venue

/**
 * Room entity for favorite venues
 * This is separate from the Venue model class to avoid Room processing issues
 */
@Entity(tableName = "favorite_venues")
data class VenueEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val isFavorite: Boolean = true,
    val lastSyncedTimestamp: Long = System.currentTimeMillis(),
    // Properties from Venue
    val name: String = "",
    val locationName: String = "",
    val image: String = "",
    val rating: Double = 0.0,
    // Sport is stored as separate primitive fields for Room compatibility
    val sportId: String? = null,
    val sportName: String? = null,
    val sportLogo: String? = null
) {
    /**
     * Convert to Venue model object
     */
    fun toVenue(): Venue {
        return Venue(
            id = id,
            name = name,
            locationName = locationName,
            image = image,
            rating = rating,
            sport = if (sportId != null) {
                Sport(
                    id = sportId,
                    name = sportName ?: "",
                    logo = sportLogo ?: ""
                )
            } else null,
            // Other fields default to null
            coords = null,
            bookings = null
        )
    }

    companion object {
        /**
         * Create VenueEntity from Venue model object
         */
        fun fromVenue(venue: Venue, userId: String): VenueEntity {
            return VenueEntity(
                id = venue.id,
                userId = userId,
                name = venue.name,
                locationName = venue.locationName,
                image = venue.image,
                rating = venue.rating,
                sportId = venue.sport?.id,
                sportName = venue.sport?.name,
                sportLogo = venue.sport?.logo
            )
        }
    }
}