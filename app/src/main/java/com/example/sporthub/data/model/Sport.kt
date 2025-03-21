// com.example.sporthub.data.model.Sport.kt
package com.example.sporthub.data.model

data class Sport(
    val id: String,
    val name: String,
    val logo: String
)

{
    // No-argument constructor required for Firestore
    constructor() : this("", "", "")
}