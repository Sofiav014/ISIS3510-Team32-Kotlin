package com.example.sporthub.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.Venue
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth // Assuming you use Firebase Auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class BookingsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _selectedDate = MutableLiveData<Date>()
    val selectedDate: LiveData<Date> = _selectedDate

    private val _bookingsForSelectedDate = MutableLiveData<List<Booking>>()
    val bookingsForSelectedDate: LiveData<List<Booking>> = _bookingsForSelectedDate

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // This will hold all bookings for the user, fetched from Firestore
    private var allUserBookings: List<Booking> = emptyList()

    init {
        // Set initial date to today
        _selectedDate.value = Calendar.getInstance().time
        // Fetch real bookings from Firestore
        fetchUserBookings()
    }

    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
        // Filter the already-fetched list for the new date
        filterBookingsForSelectedDate()
    }

    private fun fetchUserBookings() {
        // Get the current user's ID. Replace with your actual user management logic.
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("BookingsViewModel", "User not logged in.")
            // Handle not-logged-in state, maybe post an empty list or an error state
            _bookingsForSelectedDate.postValue(emptyList())
            return
        }

        _isLoading.value = true
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    // Get the raw list of booking maps from the user document
                    val bookingsRaw = doc["bookings"] as? List<Map<String, Any>>

                    // Parse the raw map list into a list of Booking data classes
                    val bookingsParsed = bookingsRaw?.mapNotNull { bookingMap ->
                        try {
                            // Also parse the nested Venue object inside each booking
                            val venueMap = bookingMap["venue"] as? Map<String, Any>
                            val venue = venueMap?.let {
                                val sportMap = it["sport"] as? Map<String, Any>
                                val sport = sportMap?.let { sp ->
                                    Sport(
                                        id = sp["id"] as? String ?: "",
                                        name = sp["name"] as? String ?: ""
                                    )
                                }
                                Venue(
                                    id = it["id"] as? String ?: "",
                                    name = it["name"] as? String ?: "",
                                    locationName = it["location_name"] as? String ?: "",
                                    image = it["image"] as? String ?: "",
                                    sport = sport
                                )
                            }

                            // Construct the Booking object
                            Booking(
                                id = bookingMap["id"] as? String ?: "",
                                startTime = bookingMap["start_time"] as? Timestamp,
                                endTime = bookingMap["end_time"] as? Timestamp,
                                maxUsers = (bookingMap["max_users"] as? Long)?.toInt() ?: 0,
                                users = bookingMap["users"] as? List<String> ?: emptyList(),
                                venue = venue // Assign the parsed venue
                            )
                        } catch (e: Exception) {
                            Log.e("BookingsViewModel", "Failed to parse a booking.", e)
                            null // Return null for this booking if parsing fails
                        }
                    } ?: emptyList()

                    allUserBookings = bookingsParsed
                    Log.d("BookingsViewModel", "Successfully fetched and parsed ${allUserBookings.size} bookings.")
                } else {
                    Log.d("BookingsViewModel", "User document does not exist.")
                    allUserBookings = emptyList()
                }
                // Now that we have the full list, filter it for the selected date
                filterBookingsForSelectedDate()
            }
            .addOnFailureListener { e ->
                Log.e("BookingsViewModel", "Error fetching user document", e)
                allUserBookings = emptyList()
                filterBookingsForSelectedDate() // Still filter to update UI to empty state
            }
    }

    private fun filterBookingsForSelectedDate() {
        // This function works the same as before, but now on real data!
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
        Log.d("BookingsViewModel", "Filtered list contains ${filtered.size} bookings for the selected date.")
    }
}