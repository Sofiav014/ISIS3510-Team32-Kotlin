package com.example.sporthub.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Parcelize
@IgnoreExtraProperties
data class Booking(
    @PropertyName("id") val id: String = "",
    @PropertyName("end_time") val endTime: @RawValue Timestamp? = null,
    @PropertyName("max_users") val maxUsers: Int = 0,
    @PropertyName("start_time") val startTime: @RawValue Timestamp? = null,
    @PropertyName("users") val users: List<String> = emptyList(),
    @PropertyName("venue") val venue: @RawValue Venue? = null
) : Parcelable
