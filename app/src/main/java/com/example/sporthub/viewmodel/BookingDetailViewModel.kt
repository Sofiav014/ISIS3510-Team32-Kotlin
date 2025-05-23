package com.example.sporthub.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.repository.BookingRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class BookingDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _booking = MutableLiveData<Booking>()
    private val repository = BookingRepository()
    val booking: LiveData<Booking> = _booking

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    sealed class JoinResult {
        object Success : JoinResult()
        data class Failure(val message: String) : JoinResult()
    }
    private val _joinResult = MutableLiveData<JoinResult>()
    val joinResult: LiveData<JoinResult> = _joinResult

    init {
        // automatically load if "bookingId" was passed in nav‐args
        savedStateHandle.get<String>("bookingId")?.let { loadBooking(it) }
    }

    fun loadBooking(id: String) {
        viewModelScope.launch {
            runCatching {
                repository.getBookingDetail(id)
            }.onSuccess { b ->
                b?.let { _booking.postValue(it) }
                    ?: _error.postValue("Booking not found")
            }.onFailure { e ->
                Log.e("BookingDetailVM","Error loading booking: ${e.message}")
            }
        }
    }

    fun joinCurrentBooking(userId: String) {
        val current = _booking.value ?: return
        viewModelScope.launch {
            if (repository.joinBooking(userId, current)) {
                _joinResult.value = JoinResult.Success
            } else {
                _joinResult.value = JoinResult.Failure("Could not join booking")
            }
        }
    }
}

