package com.example.sporthub.viewmodel

import androidx.lifecycle.*
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.repository.BookingRepository
import kotlinx.coroutines.launch


class BookingDetailViewModel(private val repository: BookingRepository = BookingRepository(),
    savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _booking = MutableLiveData<Booking>()
    val booking: LiveData<Booking> = _booking

    init {
        // automatically load if "bookingId" was passed in nav‐args
        savedStateHandle.get<String>("bookingId")?.let { loadBooking(it) }
    }

    fun loadBooking(id: String) {
        viewModelScope.launch {
            repository.getBookingDetail(id)?.let { _booking.postValue(it) }
        }
    }

    fun joinCurrentBooking(userId: String) {
        _booking.value?.let { b ->
            viewModelScope.launch {
                repository.joinBooking(userId, b)
                // reload to get updated participants list
                repository.getBookingDetail(b.id)?.let { _booking.postValue(it) }
            }
        }
    }
}
