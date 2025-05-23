package com.example.sporthub.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.Venue
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar
import java.util.Date

class BookingsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var bookingsListener: ListenerRegistration? = null

    private val _selectedDate = MutableLiveData<Date>()
    val selectedDate: LiveData<Date> = _selectedDate

    private val _bookingsForSelectedDate = MutableLiveData<List<Booking>>()
    val bookingsForSelectedDate: LiveData<List<Booking>> = _bookingsForSelectedDate

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var allUserBookings: List<Booking> = emptyList()

    init {
        _selectedDate.value = Calendar.getInstance().time
        // Start listening for real-time updates
        listenForUserBookings()
    }

    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
        filterBookingsForSelectedDate()
    }

    private fun listenForUserBookings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("BookingsViewModel", "User not logged in.")
            return
        }

        _isLoading.value = true
        val userDocRef = db.collection("users").document(userId)

        // Use addSnapshotListener for real-time updates
        bookingsListener = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("BookingsViewModel", "Listen failed.", error)
                allUserBookings = emptyList()
                filterBookingsForSelectedDate()
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val bookingsRaw = snapshot["bookings"] as? List<Map<String, Any>>
                val bookingsParsed = bookingsRaw?.mapNotNull { bookingMap ->
                    try {
                        val venueMap = bookingMap["venue"] as? Map<String, Any>
                        val venue = venueMap?.let {
                            val sportMap = it["sport"] as? Map<String, Any>
                            val sport = sportMap?.let { sp -> Sport(id = sp["id"] as? String ?: "", name = sp["name"] as? String ?: "") }
                            Venue(
                                id = it["id"] as? String ?: "",
                                name = it["name"] as? String ?: "",
                                locationName = it["location_name"] as? String ?: "",
                                image = it["image"] as? String ?: "",
                                sport = sport
                            )
                        }

                        Booking(
                            id = bookingMap["id"] as? String ?: "",
                            startTime = bookingMap["start_time"] as? Timestamp,
                            endTime = bookingMap["end_time"] as? Timestamp,
                            maxUsers = (bookingMap["max_users"] as? Long)?.toInt() ?: 0,
                            users = bookingMap["users"] as? List<String> ?: emptyList(),
                            venue = venue
                        )
                    } catch (e: Exception) {
                        Log.e("BookingsViewModel", "Failed to parse a booking.", e)
                        null
                    }
                } ?: emptyList()

                allUserBookings = bookingsParsed
                Log.d("BookingsViewModel", "Updated bookings list. Total: ${allUserBookings.size}")
            } else {
                Log.d("BookingsViewModel", "User document does not exist or is empty.")
                allUserBookings = emptyList()
            }
            // After receiving an update, filter for the currently selected date
            filterBookingsForSelectedDate()
        }
    }

    private fun filterBookingsForSelectedDate() {
        val calendar = Calendar.getInstance()
        val currentDate = _selectedDate.value ?: return
        calendar.time = currentDate

        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val filtered = allUserBookings.filter { booking ->
            booking.startTime?.toDate()?.let { bookingDate ->
                val bookingCal = Calendar.getInstance()
                bookingCal.time = bookingDate
                bookingCal.get(Calendar.YEAR) == currentYear &&
                        bookingCal.get(Calendar.MONTH) == currentMonth &&
                        bookingCal.get(Calendar.DAY_OF_MONTH) == currentDay
            } ?: false
        }
        _bookingsForSelectedDate.postValue(filtered)
        _isLoading.postValue(false)
    }

    // This is important to prevent memory leaks!
    override fun onCleared() {
        super.onCleared()
        // Stop listening for updates when the ViewModel is destroyed
        bookingsListener?.remove()
    }
}