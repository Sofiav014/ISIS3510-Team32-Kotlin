package com.example.sporthub.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Venue
import com.example.sporthub.data.repository.BookingRepository
import com.example.sporthub.utils.ConnectivityHelper
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

class CreateBookingViewModel : ViewModel() {

    private val repository = BookingRepository()

    private val _reservationResult = MutableLiveData<Boolean>()
    val reservationResult: LiveData<Boolean> get() = _reservationResult

    private val _bookingCreated = MutableLiveData<Boolean>()
    val bookingCreated: LiveData<Boolean> get() = _bookingCreated

    private val _isOffline = MutableLiveData<Boolean>()
    val isOffline: LiveData<Boolean> get() = _isOffline

    private val _bookingCreatedEvent = MutableLiveData<Unit>()
    val bookingCreatedEvent: LiveData<Unit> get() = _bookingCreatedEvent


    fun createReservation(date: String, timeSlot: String, players: Int, userId: String, venue: Venue) {
        val (startStr, endStr) = timeSlot.split(" - ").map { it.trim() }
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val start = format.parse("$date $startStr")
        val end = format.parse("$date $endStr")

        val booking = Booking(
            id = UUID.randomUUID().toString(),
            startTime = Timestamp(start!!),
            endTime = Timestamp(end!!),
            maxUsers = players,
            users = listOf(userId),
            venue = venue
        )

        viewModelScope.launch {
            try {
                // Create the booking in the database
                repository.createBooking(booking)

                // Add the booking to the user
                repository.addBookingToUser(userId, booking)

                _reservationResult.postValue(true)
                _bookingCreated.postValue(true)

                _bookingCreatedEvent.postValue(Unit)
            } catch (e: Exception) {
                _reservationResult.postValue(false)
            }
        }

    }

    // New function to check connectivity manually
    fun checkConnectivity(context: Context) {
        _isOffline.value = !ConnectivityHelper.isNetworkAvailable(context)
    }

    suspend fun addBookingToUser(userId: String, booking: Booking) {
        repository.addBookingToUser(userId, booking)
    }

    suspend fun removeBookingFromUser(userId: String, booking: Booking) {
        repository.removeBookingFromUser(userId, booking)
    }



}