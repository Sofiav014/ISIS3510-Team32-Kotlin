package com.example.sporthub.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.GeoPoint

@Parcelize
data class Venue(
    @JvmField @PropertyName("id") val id: String = "",
    @JvmField @PropertyName("coords") val coords: @RawValue GeoPoint? = null,
    @JvmField @PropertyName("image") val image: String = "",
    @JvmField @PropertyName("location_name") val locationName: String = "",
    @JvmField @PropertyName("name") val name: String = "",
    @JvmField @PropertyName("rating") val rating: Double = 0.0,
    @JvmField @PropertyName("sport") val sport: @RawValue Sport? = null,
    @JvmField @PropertyName("bookings") var bookings: @RawValue List<Booking>? = null
) : Parcelable


