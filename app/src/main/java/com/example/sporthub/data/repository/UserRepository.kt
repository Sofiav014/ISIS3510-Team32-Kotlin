// UserRepository.kt
package com.example.sporthub.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.sporthub.data.model.User
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.Venue
import com.example.sporthub.data.model.Booking
import com.google.firebase.firestore.GeoPoint
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.MutableLiveData

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getUserData(userId: String): Task<DocumentSnapshot> {
        return db.collection("users").document(userId).get()
    }

    // Method to update a single field in the user document
    fun updateUserField(userId: String, fieldName: String, value: Any?): Task<Void> {
        // This only updates the specific field without affecting other fields
        return db.collection("users").document(userId).update(fieldName, value)
    }

    fun updateUserBirthDate(userId: String, birthDate: Timestamp): Task<Void> {
        return updateUserField(userId, "birth_date", birthDate)
    }

    fun signOut() {
        auth.signOut()
    }

    fun updateUserSports(userId: String, sports: List<Map<String, Any>?>): Task<Void> {
        // Only updates the sports_liked field
        return updateUserField(userId, "sports_liked", sports)
    }

    fun createUserProfile(userId: String, userData: HashMap<String, Any>): Task<Void> {
        // This should only be used for new users, not for updating existing ones
        return db.collection("users").document(userId).set(userData)
    }

    fun updateUserName(userId: String, name: String): Task<Void> {
        return updateUserField(userId, "name", name)
    }

    fun updateUserGender(userId: String, gender: String): Task<Void> {
        return updateUserField(userId, "gender", gender)
    }

    fun updateUserProfileName(user: FirebaseUser, name: String): Task<Void> {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        return user.updateProfile(profileUpdates)
    }

    // Add this helper method to safely get timestamps
    private fun safeGetTimestamp(snapshot: DocumentSnapshot, field: String): Timestamp? {
        return try {
            snapshot.getTimestamp(field)
        } catch (e: Exception) {
            // If the field is not a Timestamp, return null
            Log.d("UserRepository", "Field $field is not a Timestamp: ${e.message}")
            null
        }
    }

    fun getUserModel(userId:String): LiveData<User>{
        val liveData = MutableLiveData<User>()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val sportsListAny = snapshot.get("sports_liked")

                    val sportsLiked = if (sportsListAny is List<*>) {
                        sportsListAny.mapNotNull { sportItem ->
                            if (sportItem is Map<*, *>) {
                                Sport(
                                    id = sportItem["id"] as? String ?: "",
                                    name = sportItem["name"] as? String ?: "",
                                    logo = sportItem["logo"] as? String ?: ""
                                )
                            } else null
                        }
                    } else emptyList()

                    val venuesLiked = (snapshot.get("venues_liked") as? List<*>)?.mapNotNull { venueItem ->
                        if (venueItem is Map<*, *>) {
                            Venue(
                                id = venueItem["id"] as? String ?: "",
                                coords = venueItem["coords"] as? GeoPoint,
                                image = venueItem["image"] as? String ?: "",
                                locationName = venueItem["location_name"] as? String ?: "",
                                name = venueItem["name"] as? String ?: "",
                                rating = (venueItem["rating"] as? Number)?.toDouble() ?: 0.0,
                                sport = (venueItem["sport"] as? Map<*, *>)?.let { sportMap ->
                                    Sport(
                                        id = sportMap["id"] as? String ?: "",
                                        name = sportMap["name"] as? String ?: "",
                                        logo = sportMap["logo"] as? String ?: ""
                                    )
                                },
                                bookings = emptyList()
                            )
                        } else null
                    } ?: emptyList()

                    val bookings = (snapshot.get("bookings") as? List<*>)?.mapNotNull { bookingItem ->
                        if (bookingItem is Map<*, *>) {
                            Booking(
                                id = bookingItem["id"] as? String ?: "",
                                endTime = bookingItem["end_time"] as? Timestamp,
                                startTime = bookingItem["start_time"] as? Timestamp,
                                maxUsers = (bookingItem["max_users"] as? Number)?.toInt() ?: 0,
                                users = (bookingItem["users"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                venue = (bookingItem["venue"] as? Map<*, *>)?.let { venueMap ->
                                    Venue(
                                        id = venueMap["id"] as? String ?: "",
                                        coords = venueMap["coords"] as? GeoPoint,
                                        image = venueMap["image"] as? String ?: "",
                                        locationName = venueMap["location_name"] as? String ?: "",
                                        name = venueMap["name"] as? String ?: "",
                                        rating = (venueMap["rating"] as? Number)?.toDouble() ?: 0.0,
                                        sport = (venueMap["sport"] as? Map<*, *>)?.let { sportMap ->
                                            Sport(
                                                id = sportMap["id"] as? String ?: "",
                                                name = sportMap["name"] as? String ?: "",
                                                logo = sportMap["logo"] as? String ?: ""
                                            )
                                        },
                                        bookings = emptyList()
                                    )
                                }
                            )
                        } else null
                    } ?: emptyList()

                    val currentUser = User(
                        id = snapshot.id,
                        name = snapshot.getString("name") ?: "",
                        gender = snapshot.getString("gender") ?: "",
                        birthDate = safeGetTimestamp(snapshot, "birth_date"),
                        sportsLiked = sportsLiked,
                        bookings = bookings,
                        venuesLiked = venuesLiked,
                    )

                    liveData.value = currentUser

                } else {
                    liveData.value = User( // Default user if document doesn't exist
                        id = "",
                        name = "",
                        gender = "",
                        birthDate = null,
                        sportsLiked = emptyList(),
                        bookings = emptyList(),
                        venuesLiked = emptyList()
                    )
                }
            }
            .addOnFailureListener {
                liveData.value = User( // Default user on failure
                    id = "",
                    name = "",
                    gender = "",
                    birthDate = null,
                    sportsLiked = emptyList(),
                    bookings = emptyList(),
                    venuesLiked = emptyList()
                )
            }
        return liveData
    }
}