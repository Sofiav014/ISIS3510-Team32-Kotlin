package com.example.sporthub.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.repository.BookingRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.content.Context
import com.example.sporthub.utils.ConnectivityHelper
import com.example.sporthub.utils.Event


class BookingDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _booking = MutableLiveData<Booking>()
    private val repository = BookingRepository()
    val booking: LiveData<Booking> = _booking

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isOffline = MutableLiveData<Boolean>()
    val isOffline: LiveData<Boolean> get() = _isOffline

    sealed class JoinResult {
        object Success : JoinResult()
        data class Failure(val message: String) : JoinResult()
    }

    private val _joinResult = MutableLiveData<Event<JoinResult>>()
    val joinResult: LiveData<Event<JoinResult>> = _joinResult

    sealed class CancelResult {
        object Success : CancelResult()
        data class Failure(val message: String) : CancelResult()
    }

    private val _cancelResult = MutableLiveData<Event<CancelResult>>()
    val cancelResult: LiveData<Event<CancelResult>> = _cancelResult

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
                Log.e("BookingDetailVM", "Error loading booking: ${e.message}")
            }
        }
    }

    fun checkConnectivity(context: Context) {
        _isOffline.value = !ConnectivityHelper.isNetworkAvailable(context)
    }

    fun joinCurrentBooking(userId: String, context: Context) {
        if (!ConnectivityHelper.isNetworkAvailable(context)) {
            _isOffline.value = true
            return
        }

        val current = _booking.value ?: return
        viewModelScope.launch {
            if (repository.joinBooking(userId, current)) {
                _joinResult.value = Event(JoinResult.Success)
                loadBooking(current.id)
            } else {
                _joinResult.value = Event(JoinResult.Failure("Could not join booking"))
            }
        }
    }

    fun cancelCurrentBooking(userId: String, context: Context) {
        if (!ConnectivityHelper.isNetworkAvailable(context)) {
            _isOffline.value = true
            return
        }

        val current = _booking.value ?: return
        viewModelScope.launch {
            try {
                val success = repository.cancelBooking(userId, current)
                if (success) {
                    _cancelResult.value = Event(CancelResult.Success)
                    // Reload the booking to get updated participant list
                    loadBooking(current.id)
                } else {
                    _cancelResult.value = Event(CancelResult.Failure("Could not cancel booking"))
                }
            } catch (e: Exception) {
                Log.e("BookingDetailVM", "Error canceling booking: ${e.message}")
                _cancelResult.value = Event(CancelResult.Failure("Error canceling booking: ${e.message}"))
            }
        }
    }
}

