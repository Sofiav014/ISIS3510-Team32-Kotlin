package com.example.sporthub.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.User
import com.example.sporthub.data.model.Venue
import com.example.sporthub.data.repository.HomeRepository
import com.example.sporthub.utils.ConnectivityHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {

    private val _recommendedBookings = MutableLiveData<List<Booking>>()
    val recommendedBookings: LiveData<List<Booking>> = _recommendedBookings

    private val _upcomingBookings = MutableLiveData<List<Booking>>()
    val upcomingBookings: LiveData<List<Booking>> = _upcomingBookings

    data class PopularityReportData(
        val highestRatedVenue: Venue?,
        val mostPlayedSport: Sport,
        val mostPlayedSportCount: Int = 0,
        val mostBookedVenue: Venue?,
        val mostBookedCount: Long = 0
    )

    private val _popularityReport = MutableLiveData<PopularityReportData>()
    val popularityReport: LiveData<PopularityReportData> = _popularityReport

    private val _isOffline = MutableLiveData<Boolean>()
    val isOffline: LiveData<Boolean> = _isOffline


    fun loadHomeData(context: Context, user: User) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("home_cache", Context.MODE_PRIVATE)
            _isOffline.value = !ConnectivityHelper.isNetworkAvailable(context)

            if (ConnectivityHelper.isNetworkAvailable(context)) {
                try {
                    val recommended = repository.getRecommendedBookings(user)
                    val upcoming = repository.getUpcomingBookings(user)
                    val report = repository.popularityReport(user)

                    _recommendedBookings.value = recommended
                    _upcomingBookings.value = upcoming
                    _popularityReport.value = mapReport(report, user)

                    // Save in caché
                    with(prefs.edit()) {
                        putString("recommended", Gson().toJson(recommended))
                        putString("upcoming", Gson().toJson(upcoming))
                        putString("report", Gson().toJson(_popularityReport.value))
                        apply()
                    }

                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error loading online data", e)
                }
            } else {
                Log.w("OfflineMode", "No internet - loading from cache")
                val recommendedJson = prefs.getString("recommended", null)
                val upcomingJson = prefs.getString("upcoming", null)
                val reportJson = prefs.getString("report", null)

                _recommendedBookings.value = recommendedJson?.let {
                    Gson().fromJson(it, object : TypeToken<List<Booking>>() {}.type)
                } ?: emptyList()

                _upcomingBookings.value = upcomingJson?.let {
                    Gson().fromJson(it, object : TypeToken<List<Booking>>() {}.type)
                } ?: emptyList()

                _popularityReport.value = reportJson?.let {
                    Gson().fromJson(it, PopularityReportData::class.java)
                } ?: PopularityReportData(null, Sport("unknown", "No sport", ""), 0, null, 0)
            }
        }
    }

    private fun mapReport(
        report: Map<String, Any?>,
        user: User
    ): PopularityReportData {
        val highestRatedVenue = report["highestRatedVenue"] as? Venue
        val mostBookedVenue = report["mostBookedVenue"] as? Venue
        val mostPlayedSport = report["mostPlayedSport"] as? Sport
        val mostBookedCount = (report["mostBookedVenueWithCount"] as? HomeRepository.VenueWithBookingCount)?.bookingCount ?: 0
        val mostPlayedSportCount = user.bookings
            .mapNotNull { it.venue?.sport }
            .count { it.id == mostPlayedSport?.id }

        return PopularityReportData(
            highestRatedVenue,
            mostPlayedSport ?: Sport("unknown", "No sport", ""),
            mostPlayedSportCount,
            mostBookedVenue,
            mostBookedCount
        )
    }
}
