package com.example.sporthub.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Venue
import com.example.sporthub.data.repository.BookingRepository
import com.example.sporthub.utils.ConnectivityHelper
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class CreateBookingViewModel : ViewModel() {

    private val repository = BookingRepository()

    private val _reservationResult = MutableLiveData<Boolean>()
    val reservationResult: LiveData<Boolean> get() = _reservationResult

    private val _bookingCreated = MutableLiveData<Boolean>()
    val bookingCreated: LiveData<Boolean> get() = _bookingCreated

    private val _isOffline = MutableLiveData<Boolean>()
    val isOffline: LiveData<Boolean> get() = _isOffline

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

        repository.createBooking(
            booking,
            onSuccess = {
                repository.addBookingToUser(userId, booking,
                    onSuccess = {
                        _reservationResult.postValue(true)
                        _bookingCreated.postValue(true)
                    },
                    onFailure = { _reservationResult.postValue(false) }
                )
            },
            onFailure = { _reservationResult.postValue(false) }
        )
    }

    // New function to check connectivity manually
    fun checkConnectivity(context: Context) {
        _isOffline.value = !ConnectivityHelper.isNetworkAvailable(context)
    }
}