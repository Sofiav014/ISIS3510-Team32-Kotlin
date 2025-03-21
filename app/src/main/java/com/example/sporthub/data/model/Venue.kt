package com.example.sporthub.data.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class Venue(
    val id: String = "",
    val coords: GeoPoint? = null,
    val image: String = "",
    @get:PropertyName("location_name") val locationName: String = "",
    val name: String = "",
    val rating: Double = 0.0,
    val sport: Sport = Sport("", "", ""),
    val bookings: List<Booking> = emptyList()
)
