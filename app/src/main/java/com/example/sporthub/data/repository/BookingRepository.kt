package com.example.sporthub.data.repository

import com.example.sporthub.data.model.Booking
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

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

    suspend fun getBookingDetail(id: String): Booking? =
        bookingsRef
            .document(id)
            .get()
            .await()
            .toObject(Booking::class.java)

    suspend fun joinBooking(userId: String, booking: Booking) {
        val bookingDoc = bookingsRef.document(booking.id)
        val userDoc    = db.collection("users").document(userId)

        withContext(Dispatchers.IO) {
            db.runBatch { batch ->
                // 1) add the user ID to the booking.users array
                batch.update(bookingDoc, "users", FieldValue.arrayUnion(userId))
                // 2) add the booking ID to the user.bookings array
                batch.update(userDoc,    "bookings", FieldValue.arrayUnion(booking.id))
            }.await()
        }
    }

}