// com.example.sporthub.data.repository.UserRepository.kt
package com.example.sporthub.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getUserData(userId: String): Task<DocumentSnapshot> {
        return db.collection("users").document(userId).get()
    }

    fun updateUserBirthDate(userId: String, birthDate: Timestamp): Task<Void> {
        return db.collection("users").document(userId).update("birth_date", birthDate)
    }

    fun signOut() {
        auth.signOut()
    }
    // Añadir a UserRepository.kt
    fun updateUserSports(userId: String, sports: List<Map<String, Any>?>): Task<Void> {
        return db.collection("users").document(userId).update("sports_liked", sports)
    }

    // Añadir a UserRepository.kt en data/repository/
    fun createUserProfile(userId: String, userData: Map<String, Any>): Task<Void> {
        return db.collection("users").document(userId).set(userData)
    }

    // Añadir a UserRepository.kt en data/repository/
    fun updateUserName(userId: String, name: String): Task<Void> {
        return db.collection("users").document(userId).update("name", name)
    }

    fun updateUserProfileName(user: FirebaseUser, name: String): Task<Void> {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        return user.updateProfile(profileUpdates)
    }

}