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

    suspend fun createBooking(booking: Booking) {
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
            "sport" to venue.sport
        )

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
                }.await()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun addBookingToUser(userId: String, booking: Booking) {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)
        try {
            userRef.update("bookings", FieldValue.arrayUnion(booking)).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getBookingDetail(id: String): Booking? = withContext(Dispatchers.IO) {
        val snap = bookingsRef.document(id).get().await()
        if (!snap.exists()) return@withContext null

        val tsStart = snap.getTimestamp("start_time")
        val tsEnd = snap.getTimestamp("end_time")
        val max = (snap.getLong("max_users") ?: 0L).toInt()
        val users = snap.get("users") as? List<String> ?: emptyList()

        val venueMap = snap.get("venue") as? Map<*, *> ?: emptyMap<Any, Any>()
        val sportMap = venueMap["sport"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val sport = Sport(
            id = sportMap["id"].toString(),
            name = sportMap["name"].toString(),
            logo = sportMap["logo"].toString()
        )

        val venue = Venue(
            id = venueMap["id"].toString(),
            coords = venueMap["coords"] as GeoPoint,
            image = venueMap["image"].toString(),
            locationName = venueMap["location_name"].toString(),
            name = venueMap["name"].toString(),
            rating = (venueMap["rating"] as? Number)?.toDouble() ?: 0.0,
            sport = sport,
            bookings = null
        )

        Booking(
            id = snap.id,
            startTime = tsStart,
            endTime = tsEnd,
            maxUsers = max,
            users = users,
            venue = venue
        )
    }

    /**
     * Join a booking - Updates all related collections atomically
     */
    suspend fun joinBooking(userId: String, booking: Booking): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val bookingRef = bookingsRef.document(booking.id)
                val userRef = db.collection("users").document(userId)
                val venueRef = booking.venue?.let { db.collection("venues").document(it.id) }

                // First, get current booking data to check if user can join
                val currentBookingDoc = bookingRef.get().await()
                if (!currentBookingDoc.exists()) {
                    Log.e("BookingRepository", "Booking does not exist")
                    return@withContext false
                }

                val currentUsers = currentBookingDoc.get("users") as? List<String> ?: emptyList()
                val maxUsers = (currentBookingDoc.getLong("max_users") ?: 0L).toInt()

                // Check if user is already in booking
                if (currentUsers.contains(userId)) {
                    Log.w("BookingRepository", "User already in booking")
                    return@withContext false
                }

                // Check if booking is full
                if (currentUsers.size >= maxUsers) {
                    Log.w("BookingRepository", "Booking is full")
                    return@withContext false
                }

                // Create updated booking object for user's bookings array
                val updatedBookingForUser = hashMapOf(
                    "id" to booking.id,
                    "end_time" to booking.endTime,
                    "max_users" to booking.maxUsers,
                    "start_time" to booking.startTime,
                    "users" to (currentUsers + userId),
                    "venue" to booking.venue?.let { venue ->
                        hashMapOf(
                            "coords" to venue.coords,
                            "id" to venue.id,
                            "image" to venue.image,
                            "location_name" to venue.locationName,
                            "name" to venue.name,
                            "rating" to venue.rating,
                            "sport" to venue.sport?.let { sport ->
                                hashMapOf(
                                    "id" to sport.id,
                                    "name" to sport.name,
                                    "logo" to sport.logo
                                )
                            }
                        )
                    }
                )

                // For venue's bookings array (simpler structure)
                val updatedBookingForVenue = hashMapOf(
                    "id" to booking.id,
                    "end_time" to booking.endTime,
                    "max_users" to booking.maxUsers,
                    "start_time" to booking.startTime,
                    "users" to (currentUsers + userId)
                )

                // Original booking data for venue (to remove)
                val originalBookingForVenue = hashMapOf(
                    "id" to booking.id,
                    "end_time" to booking.endTime,
                    "max_users" to booking.maxUsers,
                    "start_time" to booking.startTime,
                    "users" to currentUsers
                )

                db.runBatch { batch ->
                    // 1. Add user to booking's users list
                    batch.update(bookingRef, "users", FieldValue.arrayUnion(userId))

                    // 2. Add complete booking to user's bookings array
                    batch.update(userRef, "bookings", FieldValue.arrayUnion(updatedBookingForUser))

                    // 3. Update venue's bookings array if venue exists
                    venueRef?.let { venue ->
                        // Remove old booking entry
                        batch.update(venue, "bookings", FieldValue.arrayRemove(originalBookingForVenue))
                        // Add updated booking entry
                        batch.update(venue, "bookings", FieldValue.arrayUnion(updatedBookingForVenue))
                    }
                }.await()

                Log.d("BookingRepository", "User $userId successfully joined booking ${booking.id}")
                true
            } catch (e: Exception) {
                Log.e("BookingRepository", "Error joining booking: ${e.message}", e)
                false
            }
        }

    /**
     * Cancel/Leave a booking - Updates all related collections atomically
     */
    suspend fun cancelBooking(userId: String, booking: Booking): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val bookingRef = bookingsRef.document(booking.id)
                val userRef = db.collection("users").document(userId)
                val venueRef = booking.venue?.let { db.collection("venues").document(it.id) }

                // Get current booking data
                val currentBookingDoc = bookingRef.get().await()
                if (!currentBookingDoc.exists()) {
                    Log.e("BookingRepository", "Booking does not exist")
                    return@withContext false
                }

                val currentUsers = currentBookingDoc.get("users") as? List<String> ?: emptyList()

                // Check if user is in the booking
                if (!currentUsers.contains(userId)) {
                    Log.w("BookingRepository", "User not in booking")
                    return@withContext false
                }

                val updatedUsers = currentUsers.filter { it != userId }

                // Create the ENTIRE booking object to remove from user's bookings list
                // This should match exactly what's stored in the user's bookings array
                val bookingToRemoveFromUser = hashMapOf(
                    "id" to booking.id,
                    "end_time" to booking.endTime,
                    "max_users" to booking.maxUsers,
                    "start_time" to booking.startTime,
                    "users" to currentUsers, // Current users before removal
                    "venue" to booking.venue?.let { venue ->
                        hashMapOf(
                            "coords" to venue.coords,
                            "id" to venue.id,
                            "image" to venue.image,
                            "location_name" to venue.locationName,
                            "name" to venue.name,
                            "rating" to venue.rating,
                            "sport" to venue.sport?.let { sport ->
                                hashMapOf(
                                    "id" to sport.id,
                                    "name" to sport.name,
                                    "logo" to sport.logo
                                )
                            }
                        )
                    }
                )

                // Original booking in venue for removal
                val originalBookingInVenue = hashMapOf(
                    "id" to booking.id,
                    "end_time" to booking.endTime,
                    "max_users" to booking.maxUsers,
                    "start_time" to booking.startTime,
                    "users" to currentUsers
                )

                db.runBatch { batch ->
                    // 1. Remove ENTIRE booking from user's bookings array
                    batch.update(userRef, "bookings", FieldValue.arrayRemove(bookingToRemoveFromUser))

                    if (updatedUsers.isEmpty()) {
                        // 2. If no users left, delete the booking entirely
                        batch.delete(bookingRef)

                        // 3. Remove from venue's bookings
                        venueRef?.let { venue ->
                            batch.update(venue, "bookings", FieldValue.arrayRemove(originalBookingInVenue))
                        }
                    } else {
                        // 2. Update booking's users list (remove the leaving user)
                        batch.update(bookingRef, "users", updatedUsers)

                        // 3. Update venue's bookings array if venue exists
                        venueRef?.let { venue ->
                            val updatedBookingInVenue = hashMapOf(
                                "id" to booking.id,
                                "end_time" to booking.endTime,
                                "max_users" to booking.maxUsers,
                                "start_time" to booking.startTime,
                                "users" to updatedUsers // Updated users without the leaving user
                            )

                            // Remove old entry and add updated entry
                            batch.update(venue, "bookings", FieldValue.arrayRemove(originalBookingInVenue))
                            batch.update(venue, "bookings", FieldValue.arrayUnion(updatedBookingInVenue))
                        }
                    }
                }.await()

                Log.d("BookingRepository", "User $userId successfully left booking ${booking.id}")
                true
            } catch (e: Exception) {
                Log.e("BookingRepository", "Error canceling booking: ${e.message}", e)
                false
            }
        }
    }

    suspend fun removeBookingFromUser(userId: String, booking: Booking) {
        try {
            val userRef = db.collection("users").document(userId)

            // Create the exact booking object that exists in user's bookings array
            val bookingToRemove = hashMapOf(
                "id" to booking.id,
                "end_time" to booking.endTime,
                "max_users" to booking.maxUsers,
                "start_time" to booking.startTime,
                "users" to booking.users,
                "venue" to booking.venue?.let { venue ->
                    hashMapOf(
                        "coords" to venue.coords,
                        "id" to venue.id,
                        "image" to venue.image,
                        "location_name" to venue.locationName,
                        "name" to venue.name,
                        "rating" to venue.rating,
                        "sport" to venue.sport?.let { sport ->
                            hashMapOf(
                                "id" to sport.id,
                                "name" to sport.name,
                                "logo" to sport.logo
                            )
                        }
                    )
                }
            )

            userRef.update("bookings", FieldValue.arrayRemove(bookingToRemove)).await()
            Log.d("BookingRepository", "Removed booking ${booking.id} from user $userId")
        } catch (e: Exception) {
            Log.e("BookingRepository", "Error removing booking from user: ${e.message}", e)
        }
    }
}