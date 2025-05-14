package com.example.sporthub.data.model

import com.google.firebase.firestore.GeoPoint

data class CachedVenue(
    val id: String,
    val coords: GeoPoint?,
    val locationName: String,
    val name: String,
    val rating: Double,
    val sportId: String
)
