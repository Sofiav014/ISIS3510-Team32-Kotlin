package com.example.sporthub.data.repository

import android.util.Log
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.User
import com.example.sporthub.data.model.Venue
import com.example.sporthub.data.model.Sport
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import java.util.Date

class HomeRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getRecommendedBookings(user: User): List<Booking> {
        val userBookingIds = user.bookings.map { it.id }
        val sportsLikedIds = user.sportsLiked.map { it.id }

        val querySnapshot = firestore.collection("bookings")
            .whereIn("venue.sport.id", sportsLikedIds)
            .limit(10)
            .get()
            .await()

        val recommendedBookings = querySnapshot.documents.mapNotNull { doc ->
            val venueMap = doc.get("venue") as? Map<*, *> ?: return@mapNotNull null
            val sportMap = venueMap["sport"] as? Map<*, *>

            val sport = sportMap?.let {
                Sport(
                    id = it["id"] as? String ?: "",
                    name = it["name"] as? String ?: "",
                    logo = it["logo"] as? String ?: ""
                )
            }

            val venue = Venue(
                id = venueMap["id"] as? String ?: "",
                coords = venueMap["coords"] as? GeoPoint,
                imageUrl = venueMap["image"] as? String ?: "",
                locationName = venueMap["location_name"] as? String ?: "",
                name = venueMap["name"] as? String ?: "",
                rating = (venueMap["rating"] as? Number)?.toDouble() ?: 0.0,
                sport = sport,
                bookings = emptyList()
            )

            Booking(
                id = doc.getString("id") ?: doc.id,
                endTime = doc.getTimestamp("end_time"),
                startTime = doc.getTimestamp("start_time"),
                maxUsers = (doc.get("max_users") as? Number)?.toInt() ?: 0,
                users = (doc.get("users") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                venue = venue
            )
        }.filter { booking ->
            !userBookingIds.contains(booking.id) &&
                    booking.users.size < booking.maxUsers
        }.take(3)

        Log.d("HomeRepo", "recommendedBookings: $recommendedBookings")
        return recommendedBookings
    }


    fun getUpcomingBookings(user: User): List<Booking> {
        val now = Date()
        val upcommingBookings = user.bookings.filter {
            it.startTime?.toDate()?.after(now) == true
        }
        Log.d("HomeRepo", "upcommingBookings: $upcommingBookings")

        return upcommingBookings
    }

    suspend fun popularityReport(user: User): Map<String, Any?> {
        val metadataSnapshot = firestore.collection("metadata")
            .document("metadata")
            .get()
            .await()

        val data = metadataSnapshot.data ?: emptyMap<String, Any>()
        val venuesBookings = data["venues_bookings"] as? Map<String, Long> ?: emptyMap()

        val mostBookedVenueEntry = venuesBookings.maxByOrNull { it.value }

        val mostBookedVenue = mostBookedVenueEntry?.key?.let { venueId ->
            val doc = firestore.collection("venues").document(venueId).get().await()

            val sportMap = doc.get("sport") as? Map<*, *>

            val sport = sportMap?.let {
                Sport(
                    id = it["id"] as? String ?: "",
                    name = it["name"] as? String ?: "",
                    logo = it["logo"] as? String ?: ""
                )
            }

            Venue(
                id = doc.id,
                coords = doc.getGeoPoint("coords"),
                imageUrl = doc.getString("image") ?: "",
                locationName = doc.getString("location_name") ?: "",
                name = doc.getString("name") ?: "",
                rating = (doc.get("rating") as? Number)?.toDouble() ?: 0.0,
                sport = sport,
                bookings = emptyList()
            )
        }

        val highestRatedSnapshot = firestore.collection("venues")
            .orderBy("rating", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val highestRatedVenue = highestRatedSnapshot.documents.firstOrNull()?.let { doc ->
            val sportMap = doc.get("sport") as? Map<*, *>

            val sport = sportMap?.let {
                Sport(
                    id = it["id"] as? String ?: "",
                    name = it["name"] as? String ?: "",
                    logo = it["logo"] as? String ?: ""
                )
            }

            Venue(
                id = doc.id,
                coords = doc.getGeoPoint("coords"),
                imageUrl = doc.getString("image") ?: "",
                locationName = doc.getString("location_name") ?: "",
                name = doc.getString("name") ?: "",
                rating = (doc.get("rating") as? Number)?.toDouble() ?: 0.0,
                sport = sport,
                bookings = emptyList()
            )
        }

        val mostPlayedSport = if (user.bookings.isNotEmpty()) {
            user.bookings
                .groupBy { it.venue?.sport?.id }
                .maxByOrNull { it.value.size }
                ?.value?.firstOrNull()?.venue?.sport
        } else {
            null
        }

        return mapOf(
            "highestRatedVenue" to highestRatedVenue,
            "mostBookedVenue" to mostBookedVenue,
            "mostPlayedSport" to mostPlayedSport
        )
    }

}
