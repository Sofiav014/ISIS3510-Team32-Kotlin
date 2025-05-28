package com.example.sporthub.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.Venue
import com.example.sporthub.utils.ConnectivityHelper
import com.example.sporthub.utils.LRUCache // Import your LRUCache
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BookingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences: SharedPreferences = application.getSharedPreferences(
        "bookings_preferences",
        Context.MODE_PRIVATE
    )
    companion object {
        private const val KEY_LAST_DATE = "key_last_date"
    }

    // Instantiate the LRUCache to hold daily bookings.
    private val bookingsCache = LRUCache<String, List<Booking>>(5) // Caches up to 5 days
    // A formatter to create consistent keys from dates (e.g., "2025-05-28")
    private val cacheKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // --- END OF NEW CACHING IMPLEMENTATION ---


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
        // This logic is unchanged
        val lastDateMillis = sharedPreferences.getLong(KEY_LAST_DATE, -1L)
        _selectedDate.value = if (lastDateMillis != -1L) Date(lastDateMillis) else Calendar.getInstance().time
        listenForUserBookings()
    }

    fun onRetry() {
        listenForUserBookings()
    }


    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
        sharedPreferences.edit().putLong(KEY_LAST_DATE, date.time).apply()

        // Decide what to do based on connectivity
        if (isNetworkAvailable.value == true) {
            // If online, filter the master list from Firestore which will also update the cache
            filterBookingsForSelectedDate()
        } else {
            // If offline, try to load the newly selected date directly from the cache
            loadFromCache()
        }
    }


    private fun listenForUserBookings() {
        if (!ConnectivityHelper.isNetworkAvailable(getApplication())) {
            _isNetworkAvailable.postValue(false)
            _isLoading.postValue(false)
            // When offline, immediately attempt to load data from the cache
            loadFromCache()
            return
        }
        _isNetworkAvailable.postValue(true)

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
        val currentDate = _selectedDate.value ?: return
        val calendar = Calendar.getInstance().apply { time = currentDate }
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val filtered = allUserBookings.filter { booking ->
            booking.startTime?.toDate()?.let { bookingDate ->
                val bookingCal = Calendar.getInstance().apply { time = bookingDate }
                bookingCal.get(Calendar.YEAR) == currentYear &&
                        bookingCal.get(Calendar.MONTH) == currentMonth &&
                        bookingCal.get(Calendar.DAY_OF_MONTH) == currentDay
            } ?: false
        }

        // Save the filtered list to our cache
        val cacheKey = cacheKeyFormatter.format(currentDate)
        bookingsCache[cacheKey] = filtered
        Log.d("BookingsViewModel", "Saved ${filtered.size} bookings to cache for key: $cacheKey")

        _bookingsForSelectedDate.postValue(filtered)
        _isLoading.postValue(false)
    }


    private fun loadFromCache() {
        val currentDate = _selectedDate.value ?: return
        val cacheKey = cacheKeyFormatter.format(currentDate)

        // cachedBookings is of type List<Booking>? (nullable)
        val cachedBookings = bookingsCache[cacheKey]

        // This 'if' check is the key to solving the error
        if (cachedBookings != null) {
            // Inside this block, Kotlin knows cachedBookings is NOT null
            _bookingsForSelectedDate.postValue(cachedBookings) // This is now safe
            Log.d("BookingsViewModel", "Loaded ${cachedBookings.size} bookings from cache for key: $cacheKey")
        } else {
            // If the cache returned null, we post an empty list instead
            _bookingsForSelectedDate.postValue(emptyList())
            Log.d("BookingsViewModel", "No bookings found in cache for key: $cacheKey")
        }
    }

    override fun onCleared() {
        super.onCleared()
        bookingsListener?.remove()
    }
}