package com.example.sporthub.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.User
import com.example.sporthub.data.model.Venue
import com.example.sporthub.data.repository.HomeRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {

    private val _recommendedBookings = MutableLiveData<List<Booking>>()
    val recommendedBookings: LiveData<List<Booking>> = _recommendedBookings

    private val _upcomingBookings = MutableLiveData<List<Booking>>()
    val upcomingBookings: LiveData<List<Booking>> = _upcomingBookings

    private val _popularityReport = MutableLiveData<Triple<Venue?, Sport, Venue?>>()
    val popularityReport: LiveData<Triple<Venue?, Sport, Venue?>> = _popularityReport

    fun loadData(user: User) {
        viewModelScope.launch {
            try {
                _recommendedBookings.value = repository.getRecommendedBookings(user)
                _upcomingBookings.value = repository.getUpcomingBookings(user)

                val report = repository.popularityReport(user)

                val highestRatedVenue = report["highestRatedVenue"] as? Venue
                val mostBookedVenue = report["mostBookedVenue"] as? Venue
                val mostPlayedSport = report["mostPlayedSport"] as? Sport


                Log.d("PopularityReport", "Fetched: $highestRatedVenue, $mostPlayedSport, $mostBookedVenue")


                _popularityReport.value = Triple(
                    highestRatedVenue,
                    mostPlayedSport ?: Sport(id = "unknown", name = "No sport found", logo = ""),
                    mostBookedVenue
                )

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("PopularityReport", "Error fetching data", e)
            }
        }
    }
}
