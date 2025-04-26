package com.example.sporthub.data.repository

import com.example.sporthub.data.model.Booking
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class BookingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val bookingsRef = db.collection("bookings")


    fun createBooking(
        booking: Booking,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val bookingRef = db.collection("bookings").document(booking.id)
        val userId = booking.users.firstOrNull()
        val venue = booking.venue

        if (userId == null || venue == null) {
            onFailure(IllegalArgumentException("Booking must have a user and venue"))
            return
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

        db.runBatch { batch ->
            batch.set(bookingRef, bookingData)
            batch.update(userRef, "bookings", FieldValue.arrayUnion(bookingData))
            batch.update(venueRef, "bookings", FieldValue.arrayUnion(bookingVenueData))

            venue.sport?.name?.let { sportName ->
                batch.update(metadataRef, "sports_bookings.$sportName", FieldValue.increment(1))
            }

            batch.update(metadataRef, "venues_bookings.${venue.id}", FieldValue.increment(1))

        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    fun addBookingToVenue(venueId: String, booking: Booking, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val venueRef = FirebaseFirestore.getInstance().collection("venues").document(venueId)
        venueRef.update("bookings", FieldValue.arrayUnion(booking))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun addBookingToUser(userId: String, booking: Booking, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)
        userRef.update("bookings", FieldValue.arrayUnion(booking))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }



}