// com.example.sporthub.data.model.User.kt
package com.example.sporthub.data.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val gender: String = "",
    val birthDate: Timestamp? = null,
    val sportsLiked: List<Map<String, Any>> = emptyList(),
    val bookings: List<String> = emptyList(),
    val venuesLiked: List<String> = emptyList()
)