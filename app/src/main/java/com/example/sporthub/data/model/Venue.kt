package com.example.sporthub.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.GeoPoint


data class Venue(
    @PropertyName("id") val id: String = "",
    @PropertyName("coords") val coords: GeoPoint? = null,
    @PropertyName("image") val image: String ="",
    @PropertyName("location_name") val locationName: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("rating") val rating: Double = 0.0,
    @PropertyName("sport") val sport: Sport? = null,
    @PropertyName("bookings") var bookings: List<Booking> = listOf()
)
