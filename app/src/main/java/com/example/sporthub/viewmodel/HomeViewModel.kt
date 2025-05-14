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
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.sporthub.R

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

    private val cache = LruCache<String, String>(10 * 1024 * 1024) // 10MB cache for shared preferences data

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
                    cache.put("recommended", Gson().toJson(recommended))
                    cache.put("upcoming", Gson().toJson(upcoming))
                    cache.put("report", Gson().toJson(_popularityReport.value))

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
                val recommendedJson = cache.get("recommended")
                val upcomingJson = cache.get("upcoming")
                val reportJson = cache.get("report")

                if (recommendedJson != null && upcomingJson != null && reportJson != null) {
                    _recommendedBookings.value = Gson().fromJson(recommendedJson, object : TypeToken<List<Booking>>() {}.type)
                    _upcomingBookings.value = Gson().fromJson(upcomingJson, object : TypeToken<List<Booking>>() {}.type)
                    _popularityReport.value = Gson().fromJson(reportJson, PopularityReportData::class.java)
                } else {
                    // Load from SharedPreferences if not found in LRU cache
                    val recommended = prefs.getString("recommended", null)
                    val upcoming = prefs.getString("upcoming", null)
                    val report = prefs.getString("report", null)

                    _recommendedBookings.value = recommended?.let {
                        Gson().fromJson(it, object : TypeToken<List<Booking>>() {}.type)
                    } ?: emptyList()

                    _upcomingBookings.value = upcoming?.let {
                        Gson().fromJson(it, object : TypeToken<List<Booking>>() {}.type)
                    } ?: emptyList()

                    _popularityReport.value = report?.let {
                        Gson().fromJson(it, PopularityReportData::class.java)
                    } ?: PopularityReportData(null, Sport("unknown", "No sport", ""), 0, null, 0)
                }
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


    fun loadImageIntoImageView(context: Context, imageUrl: String, imageView: ImageView) {
        Glide.with(context)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .error(R.drawable.placeholder_image)
            .into(imageView)  // Directly load into ImageView
    }

}
