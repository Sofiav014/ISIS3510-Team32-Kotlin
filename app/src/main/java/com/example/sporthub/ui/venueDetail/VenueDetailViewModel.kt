package com.example.sporthub.ui.venueDetail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Venue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class VenueDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _venue = MutableLiveData<Venue?>()
    val venue: LiveData<Venue?> get() = _venue

    fun fetchVenueById(venueId: String) {
        db.collection("venues").document(venueId)
            .get()
            .addOnSuccessListener { doc ->

                val rawVenue = doc.toObject(Venue::class.java)?.copy(id = doc.id)

                val bookingsRaw = doc["bookings"] as? List<Map<String, Any>>

                val bookingsParsed = bookingsRaw?.mapNotNull { map ->
                    try {
                        Booking(
                            id = map["id"] as? String ?: "",
                            startTime = map["start_time"] as? Timestamp,
                            endTime = map["end_time"] as? Timestamp,
                            maxUsers = (map["max_users"] as? Long)?.toInt() ?: 0,
                            users = map["users"] as? List<String> ?: emptyList()
                        )

                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                rawVenue?.bookings = filterUpcomingBookings(bookingsParsed)
                _venue.value = rawVenue

                Log.d("DEBUG", "Parsed bookings count: ${bookingsParsed.size}")
            }
            .addOnFailureListener {
                _venue.value = null
            }
    }
    fun setVenue(venue: Venue) {
        _venue.value = venue
    }


    fun setVenueFromCache(cachedVenue: Venue) {
        _venue.value = cachedVenue
    }

    private fun filterUpcomingBookings(bookings: List<Booking>): List<Booking> {
        val now = Date()
        return bookings.filter { booking ->
            val startAfterNow = booking.startTime?.toDate()?.after(now) == true
            val hasAvailableSpots = booking.users.size < booking.maxUsers
            startAfterNow && hasAvailableSpots
        }
    }



}
