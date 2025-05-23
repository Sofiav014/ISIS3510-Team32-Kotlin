package com.example.sporthub.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Venue
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class VenueDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _venue = MutableLiveData<Venue?>()
    val venue: LiveData<Venue?> get() = _venue

    private val _isFavorite = MutableLiveData<Boolean>()
    val isFavorite: LiveData<Boolean> get() = _isFavorite

    private val currentUser = Firebase.auth.currentUser
    private val usersCollection = db.collection("users")

    // Use the correct field name from your User data model
    private val LIKED_VENUES_FIELD = "venues_liked"

    /**
     * Checks if the venue is in the current user's list of liked venues.
     */
    fun checkFavoriteStatus(venueId: String) {
        if (currentUser == null) return

        usersCollection.document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Firestore reads arrays of custom objects as a List of Maps.
                    val likedVenuesList = document[LIKED_VENUES_FIELD] as? List<Map<String, Any>>
                    // Check if any map in the list has an "id" field that matches our venueId.
                    val isLiked = likedVenuesList?.any { venueMap ->
                        venueMap["id"] == venueId
                    } == true
                    _isFavorite.value = isLiked
                } else {
                    _isFavorite.value = false
                }
            }
            .addOnFailureListener {
                _isFavorite.value = false
                Log.e("VenueDetailViewModel", "Error checking favorite status", it)
            }
    }

    /**
     * Adds or removes a venue from the user's favorites list in Firestore.
     */
    fun toggleFavoriteStatus() {
        val currentVenue = _venue.value ?: return // We need the full venue object to work with
        if (currentUser == null) {
            Log.w("VenueDetailViewModel", "Cannot toggle favorite, user is not logged in.")
            return
        }

        val userDocRef = usersCollection.document(currentUser.uid)
        val isCurrentlyFavorite = _isFavorite.value == true

        if (isCurrentlyFavorite) {
            // Use a transaction to safely find the venue by ID and remove it from the array.
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userDocRef)
                val likedVenues = snapshot[LIKED_VENUES_FIELD] as? List<HashMap<String, Any>> ?: emptyList()
                val newLikedVenues = likedVenues.filterNot { it["id"] == currentVenue.id }
                transaction.update(userDocRef, LIKED_VENUES_FIELD, newLikedVenues)
            }.addOnSuccessListener {
                _isFavorite.postValue(false)
                Log.d("VenueDetailViewModel", "Venue removed from favorites.")
            }.addOnFailureListener { e ->
                Log.e("VenueDetailViewModel", "Failed to remove favorite", e)
            }
        } else {
            // --- ADD LOGIC ---
            // Create a clean Venue object to add. It's good practice to not store
            // nested, volatile data like bookings within the user's liked list.
            val venueToAdd = currentVenue.copy(bookings = null)
            userDocRef.update(LIKED_VENUES_FIELD, FieldValue.arrayUnion(venueToAdd))
                .addOnSuccessListener {
                    _isFavorite.value = true
                    Log.d("VenueDetailViewModel", "Venue added to favorites.")
                }.addOnFailureListener { e ->
                    Log.e("VenueDetailViewModel", "Failed to add favorite", e)
                }
        }
    }

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

                // After fetching the venue, check its favorite status
                checkFavoriteStatus(venueId)

                Log.d("DEBUG", "Parsed bookings count: ${bookingsParsed.size}")
            }
            .addOnFailureListener {
                _venue.value = null
            }
    }

    fun setVenue(venue: Venue) {
        _venue.value = venue
        // When setting venue from args, also check its favorite status
        checkFavoriteStatus(venue.id)
    }

    fun setVenueFromCache(cachedVenue: Venue) {
        _venue.value = cachedVenue
        // Also check favorite status for cached venues
        checkFavoriteStatus(cachedVenue.id)
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
