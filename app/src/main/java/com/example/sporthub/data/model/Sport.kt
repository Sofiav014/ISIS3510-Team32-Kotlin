package com.example.sporthub.data.model

import com.google.firebase.firestore.PropertyName

data class Sport(
    @PropertyName("id") val id: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("logo") val logo: String = ""
)

{
    // No-argument constructor required for Firestore
    constructor() : this("", "", "")
}