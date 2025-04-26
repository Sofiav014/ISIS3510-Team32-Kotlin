package com.example.sporthub.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.PropertyName

@Parcelize
data class Sport(
    @PropertyName("id") val id: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("logo") val logo: String = ""
) : Parcelable
