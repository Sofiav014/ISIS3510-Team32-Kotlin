package com.example.sporthub.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.Venue
import com.example.sporthub.utils.ConnectivityHelper // Import your helper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar
import java.util.Date

class BookingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences: SharedPreferences = application.getSharedPreferences(
        "bookings_preferences", // Name for your preferences file
        Context.MODE_PRIVATE
    )
    companion object {
        private const val KEY_LAST_DATE = "key_last_date"
    }


    private val db = FirebaseFirestore.getInstance()
    private var bookingsListener: ListenerRegistration? = null

    private val _isNetworkAvailable = MutableLiveData<Boolean>()
    val isNetworkAvailable: LiveData<Boolean> = _isNetworkAvailable

    private val _selectedDate = MutableLiveData<Date>()
    val selectedDate: LiveData<Date> = _selectedDate

    private val _bookingsForSelectedDate = MutableLiveData<List<Booking>>()
    val bookingsForSelectedDate: LiveData<List<Booking>> = _bookingsForSelectedDate

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var allUserBookings: List<Booking> = emptyList()

    init {
        // Read the last saved date directly from SharedPreferences
        val lastDateMillis = sharedPreferences.getLong(KEY_LAST_DATE, -1L)

        _selectedDate.value = if (lastDateMillis != -1L) {
            // If a date was saved, use it
            Date(lastDateMillis)
        } else {
            // Otherwise, default to today
            Calendar.getInstance().time
        }

        // Now that the date is set, fetch bookings
        listenForUserBookings()
    }

    fun onRetry() {
        listenForUserBookings()
    }

    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
        // Save the new date to SharedPreferences using the classic apply() method
        sharedPreferences.edit()
            .putLong(KEY_LAST_DATE, date.time)
            .apply()

        filterBookingsForSelectedDate()
    }

    private fun listenForUserBookings() {

        if (!ConnectivityHelper.isNetworkAvailable(getApplication())) {
            _isNetworkAvailable.postValue(false)
            // Post empty values to clear screen and stop loading indicators
            _bookingsForSelectedDate.postValue(emptyList())
            _isLoading.postValue(false)
            return // Stop here if no network
        }
        _isNetworkAvailable.postValue(true) // Network is available

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("BookingsViewModel", "User not logged in.")
            return
        }

        _isLoading.value = true
        val userDocRef = db.collection("users").document(userId)

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
            } else {
                allUserBookings = emptyList()
            }
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

    override fun onCleared() {
        super.onCleared()
        bookingsListener?.remove()
    }
}