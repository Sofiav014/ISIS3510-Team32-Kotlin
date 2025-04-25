package com.example.sporthub.data.repository

import com.example.sporthub.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore

class BookingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val bookingsRef = db.collection("bookings")

    fun createBooking(
        booking: Booking,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        bookingsRef.document(booking.id)
            .set(booking)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
}