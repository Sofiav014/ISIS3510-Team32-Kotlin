package com.example.sporthub.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.Venue
import com.example.sporthub.utils.ConnectivityHelper
import com.example.sporthub.utils.LRUCache
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val bookingsCache = LRUCache<String, List<Booking>>(5)
    private val cacheKeyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
        viewModelScope.launch {
            // Use IO dispatcher for SharedPreferences read
            val initialDate = withContext(Dispatchers.IO) {
                val lastDateMillis = sharedPreferences.getLong(KEY_LAST_DATE, -1L)
                if (lastDateMillis != -1L) Date(lastDateMillis) else Calendar.getInstance().time
            }
            // Update LiveData on the Main thread
            _selectedDate.value = initialDate
            listenForUserBookings()
        }
    }

    fun onRetry() {
        listenForUserBookings()
    }

    fun setSelectedDate(date: Date) {
        _selectedDate.value = date
        // Launch a coroutine for the SharedPreferences I/O operation
        viewModelScope.launch(Dispatchers.IO) {
            sharedPreferences.edit().putLong(KEY_LAST_DATE, date.time).apply()
        }

        if (isNetworkAvailable.value == true) {
            viewModelScope.launch { filterBookingsForSelectedDate() }
        } else {
            viewModelScope.launch { loadFromCache() }
        }
    }

    private fun listenForUserBookings() {
        if (!ConnectivityHelper.isNetworkAvailable(getApplication())) {
            _isNetworkAvailable.postValue(false)
            _isLoading.postValue(false)
            viewModelScope.launch { loadFromCache() }
            return
        }
        _isNetworkAvailable.postValue(true)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("BookingsViewModel", "User not logged in."); return
        }

        _isLoading.value = true
        val userDocRef = db.collection("users").document(userId)

        bookingsListener = userDocRef.addSnapshotListener { snapshot, error ->
            viewModelScope.launch {
                if (error != null) {
                    Log.e("BookingsViewModel", "Listen failed.", error)
                    allUserBookings = emptyList()
                    filterBookingsForSelectedDate()
                    return@launch
                }

                // Use IO dispatcher to move parsing off the main thread
                val parsedList = withContext(Dispatchers.IO) {
                    if (snapshot != null && snapshot.exists()) {
                        val bookingsRaw = snapshot["bookings"] as? List<Map<String, Any>>
                        bookingsRaw?.mapNotNull { bookingMap ->
                            try {
                                val venueMap = bookingMap["venue"] as? Map<String, Any>
                                val venue = venueMap?.let {
                                    val sportMap = it["sport"] as? Map<String, Any>
                                    val sport = sportMap?.let { sp -> Sport(id = sp["id"] as? String ?: "", name = sp["name"] as? String ?: "") }
                                    Venue(id = it["id"] as? String ?: "", name = it["name"] as? String ?: "", locationName = it["location_name"] as? String ?: "", image = it["image"] as? String ?: "", sport = sport)
                                }
                                Booking(id = bookingMap["id"] as? String ?: "", startTime = bookingMap["start_time"] as? Timestamp, endTime = bookingMap["end_time"] as? Timestamp, maxUsers = (bookingMap["max_users"] as? Long)?.toInt() ?: 0, users = bookingMap["users"] as? List<String> ?: emptyList(), venue = venue)
                            } catch (e: Exception) {
                                Log.e("BookingsViewModel", "Failed to parse a booking.", e); null
                            }
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
                allUserBookings = parsedList
                filterBookingsForSelectedDate()
            }
        }
    }

    private suspend fun filterBookingsForSelectedDate() {
        val currentDate = _selectedDate.value ?: return

        // Use IO dispatcher to move filtering off the main thread
        val filtered = withContext(Dispatchers.IO) {
            val calendar = Calendar.getInstance().apply { time = currentDate }
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

            val filteredList = allUserBookings.filter { booking ->
                booking.startTime?.toDate()?.let { bookingDate ->
                    val bookingCal = Calendar.getInstance().apply { time = bookingDate }
                    bookingCal.get(Calendar.YEAR) == currentYear &&
                            bookingCal.get(Calendar.MONTH) == currentMonth &&
                            bookingCal.get(Calendar.DAY_OF_MONTH) == currentDay
                } ?: false
            }

            val cacheKey = cacheKeyFormatter.format(currentDate)
            bookingsCache[cacheKey] = filteredList
            Log.d("BookingsViewModel", "Saved ${filteredList.size} bookings to cache for key: $cacheKey")
            filteredList
        }

        _bookingsForSelectedDate.postValue(filtered)
        _isLoading.postValue(false)
    }

    private suspend fun loadFromCache() {
        // Use IO dispatcher for consistency in background work
        val cachedBookings = withContext(Dispatchers.IO) {
            val currentDate = _selectedDate.value ?: return@withContext null
            val cacheKey = cacheKeyFormatter.format(currentDate)
            bookingsCache[cacheKey]
        }

        if (cachedBookings != null) {
            _bookingsForSelectedDate.postValue(cachedBookings)
            Log.d("BookingsViewModel", "Loaded ${cachedBookings.size} bookings from cache")
        } else {
            _bookingsForSelectedDate.postValue(emptyList())
            Log.d("BookingsViewModel", "No bookings found in cache")
        }
    }

    override fun onCleared() {
        super.onCleared()
        bookingsListener?.remove()
    }
}