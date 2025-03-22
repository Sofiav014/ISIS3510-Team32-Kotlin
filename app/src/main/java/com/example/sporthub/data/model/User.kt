// com.example.sporthub.data.model.User.kt
package com.example.sporthub.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName


data class User(
    @PropertyName("id") val id: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("gender") val gender: String = "",
    @PropertyName("birth_date") val birthDate: Timestamp? = null,
    @PropertyName("sports_liked") val sportsLiked: List<Sport> = emptyList(),
    @PropertyName("bookings") val bookings: List<Booking> = emptyList(),
    @PropertyName("venues_liked") val venuesLiked: List<Venue> = listOf()
)