package com.example.sporthub.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class Booking(
    @PropertyName("id") val id: String = "",
    @PropertyName("end_time") val endTime: Timestamp? = null,
    @PropertyName("max_users") val maxUsers: Int = 0,
    @PropertyName("start_time") val startTime: Timestamp? = null,
    @PropertyName("users") val users: List<String> = emptyList(),
    @PropertyName("venue") val venue: Venue? = null
)
