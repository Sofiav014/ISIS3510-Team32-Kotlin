package com.example.sporthub.data.model

import com.google.firebase.database.PropertyName

data class Booking(
    val id: String = "",
    @get:PropertyName("end_time") val endTime: com.google.firebase.Timestamp? = null,
    @get:PropertyName("start_time") val startTime: com.google.firebase.Timestamp? = null,
    @get:PropertyName("max_users") val maxUsers: Int = 0,
    val users: List<String> = emptyList()
)
