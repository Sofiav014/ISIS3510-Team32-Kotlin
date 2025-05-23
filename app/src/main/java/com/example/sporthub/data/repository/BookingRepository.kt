package com.example.sporthub.data.repository

import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.Venue
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import android.util.Log

class BookingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val bookingsRef = db.collection("bookings")


    suspend fun createBooking(
        booking: Booking) {
        val bookingRef = db.collection("bookings").document(booking.id)
        val userId = booking.users.firstOrNull()
        val venue = booking.venue

        if (userId == null || venue == null) {
            throw IllegalArgumentException("Booking must have a user and venue")
        }

        val userRef = db.collection("users").document(userId)
        val venueRef = db.collection("venues").document(venue.id)
        val metadataRef = db.collection("metadata").document("metadata")

        val venueInfo = hashMapOf(
            "coords" to venue.coords,
            "id" to venue.id,
            "image" to venue.image,
            "location_name" to venue.locationName,
            "name" to venue.name,
            "rating" to venue.rating,
            "sport" to venue.sport)


        val bookingData = hashMapOf(
            "end_time" to booking.endTime,
            "max_users" to booking.maxUsers,
            "start_time" to booking.startTime,
            "users" to listOf(userId),
            "venue" to venueInfo
        )

        val bookingVenueData = hashMapOf(
            "id" to booking.id,
            "end_time" to booking.endTime,
            "max_users" to booking.maxUsers,
            "start_time" to booking.startTime,
            "users" to listOf(userId),
        )

        withContext(Dispatchers.IO) {
            try {
                db.runBatch { batch ->
                    batch.set(bookingRef, bookingData)
                    batch.update(userRef, "bookings", FieldValue.arrayUnion(bookingData))
                    batch.update(venueRef, "bookings", FieldValue.arrayUnion(bookingVenueData))

                    venue.sport?.name?.let { sportName ->
                        batch.update(metadataRef, "sports_bookings.$sportName", FieldValue.increment(1))
                    }

                    batch.update(metadataRef, "venues_bookings.${venue.id}", FieldValue.increment(1))
                }.await()  // This will suspend until the batch operation is complete
            } catch (e: Exception) {
                throw e  // Rethrow to handle in the calling function (e.g., in ViewModel)
            }
        }
    }


    suspend fun addBookingToUser(userId: String, booking: Booking) {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)
        try {
            userRef.update("bookings", FieldValue.arrayUnion(booking)).await()
        } catch (e: Exception) {
            throw e  // Rethrow exception to be handled in ViewModel or calling function
        }
    }

    suspend fun getBookingDetail(id: String): Booking? = withContext(Dispatchers.IO) {
        val snap = bookingsRef.document(id).get().await()
        if (!snap.exists()) return@withContext null

        val tsStart = snap.getTimestamp("start_time")
        val tsEnd   = snap.getTimestamp("end_time")
        val max     = (snap.getLong("max_users") ?: 0L).toInt()
        val users   = snap.get("users") as? List<String> ?: emptyList()

        val venueMap = snap.get("venue") as? Map<*, *> ?: emptyMap<Any,Any>()
        val sportMap = venueMap["sport"] as? Map<*, *> ?: emptyMap<Any,Any>()
        val sport = Sport(
            id   = sportMap["id"].toString(),
            name = sportMap["name"].toString(),
            logo = sportMap["logo"].toString()
        )

        val venue = Venue(
            id           = venueMap["id"].toString(),
            coords       = venueMap["coords"] as GeoPoint,
            image        = venueMap["image"].toString(),
            locationName = venueMap["location_name"].toString(),
            name         = venueMap["name"].toString(),
            rating       = (venueMap["rating"] as? Number)?.toDouble() ?: 0.0,
            sport        = sport,
            bookings     = null
        )

        Booking(
            id        = snap.id,
            startTime = tsStart,
            endTime   = tsEnd,
            maxUsers  = max,
            users     = users,
            venue     = venue
        )
    }



    suspend fun joinBooking(userId: String, booking: Booking): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val bookingDoc = bookingsRef.document(booking.id)
                val userDoc    = db.collection("users").document(userId)
                db.runBatch { batch ->
                    batch.update(bookingDoc, "users", FieldValue.arrayUnion(userId))
                    batch.update(userDoc,    "bookings", FieldValue.arrayUnion(booking))
                }.await()
            }.isSuccess
        }
    suspend fun cancelBooking(userId: String, booking: Booking): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()

            // Start a batch operation to ensure all operations succeed or fail together
            val batch = firestore.batch()

            // 1. Remove user from booking's users list
            val bookingRef = firestore.collection("bookings").document(booking.id)
            batch.update(bookingRef, "users", FieldValue.arrayRemove(userId))

            // 2. Remove booking from user's bookings list
            val userRef = firestore.collection("users").document(userId)
            batch.update(userRef, "bookings", FieldValue.arrayRemove(booking.id))

            // 3. If the booking has a venue, remove user from venue's booking participants
            booking.venue?.id?.let { venueId ->
                val venueRef = firestore.collection("venues").document(venueId)

                // Create the booking data structure that matches what's stored in venue
                val bookingVenueData = hashMapOf(
                    "id" to booking.id,
                    "end_time" to booking.endTime,
                    "max_users" to booking.maxUsers,
                    "start_time" to booking.startTime,
                    "users" to booking.users.filter { it != userId }, // Remove the user from the list
                )

                // Remove the old booking data and add the updated one
                val oldBookingVenueData = hashMapOf(
                    "id" to booking.id,
                    "end_time" to booking.endTime,
                    "max_users" to booking.maxUsers,
                    "start_time" to booking.startTime,
                    "users" to booking.users,
                )

                batch.update(venueRef, "bookings", FieldValue.arrayRemove(oldBookingVenueData))

                // Only add back if there are still users in the booking
                if (booking.users.size > 1) {
                    batch.update(venueRef, "bookings", FieldValue.arrayUnion(bookingVenueData))
                }
            }

            // Execute all operations
            batch.commit().await()
            true

        } catch (e: Exception) {
            Log.e("BookingRepository", "Error canceling booking: ${e.message}")
            false
        }
    }


    suspend fun removeBookingFromUser(userId: String, booking: Booking) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val userRef = firestore.collection("users").document(userId)

            // Remove booking ID from user's bookings array
            userRef.update("bookings", FieldValue.arrayRemove(booking.id)).await()

        } catch (e: Exception) {
            Log.e("CreateBookingViewModel", "Error removing booking from user: ${e.message}")
        }
    }
}