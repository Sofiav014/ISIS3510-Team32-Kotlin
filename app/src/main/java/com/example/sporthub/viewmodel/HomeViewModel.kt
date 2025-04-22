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

    // Update the data structure to include sport play count
    data class PopularityReportData(
        val highestRatedVenue: Venue?,
        val mostPlayedSport: Sport,
        val mostPlayedSportCount: Int = 0,
        val mostBookedVenue: Venue?,
        val mostBookedCount: Long = 0
    )

    private val _popularityReport = MutableLiveData<PopularityReportData>()
    val popularityReport: LiveData<PopularityReportData> = _popularityReport

    fun loadData(user: User) {
        viewModelScope.launch {
            try {
                _recommendedBookings.value = repository.getRecommendedBookings(user)
                _upcomingBookings.value = repository.getUpcomingBookings(user)

                val report = repository.popularityReport(user)

                val highestRatedVenue = report["highestRatedVenue"] as? Venue
                val mostBookedVenue = report["mostBookedVenue"] as? Venue
                val mostPlayedSport = report["mostPlayedSport"] as? Sport

                // Get the most booked venue with count
                val mostBookedVenueWithCount = report["mostBookedVenueWithCount"] as? HomeRepository.VenueWithBookingCount
                val mostBookedCount = mostBookedVenueWithCount?.bookingCount ?: 0

                // Calculate number of times user played the most played sport
                var mostPlayedSportCount = 0
                if (mostPlayedSport != null && mostPlayedSport.id != "unknown") {
                    mostPlayedSportCount = user.bookings
                        .mapNotNull { it.venue?.sport }
                        .count { it.id == mostPlayedSport.id }
                }

                Log.d("PopularityReport", "Fetched: $highestRatedVenue, $mostPlayedSport (played $mostPlayedSportCount times), $mostBookedVenue")

                _popularityReport.value = PopularityReportData(
                    highestRatedVenue,
                    mostPlayedSport ?: Sport(id = "unknown", name = "No sport found", logo = ""),
                    mostPlayedSportCount,
                    mostBookedVenue,
                    mostBookedCount
                )

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("PopularityReport", "Error fetching data", e)
            }
        }
    }
}