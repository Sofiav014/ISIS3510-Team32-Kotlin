package com.example.sporthub.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.repository.UserRepository
import java.util.ArrayList

class GenderSelectionViewModel : ViewModel() {
    private val repository = UserRepository()
    private val TAG = "GenderSelectionViewModel"

    private val _saveSuccessEvent = MutableLiveData<Boolean>()
    val saveSuccessEvent: LiveData<Boolean> = _saveSuccessEvent

    private val _errorEvent = MutableLiveData<String>()
    val errorEvent: LiveData<String> = _errorEvent

    private val _userNotAuthenticatedEvent = MutableLiveData<Boolean>()
    val userNotAuthenticatedEvent: LiveData<Boolean> = _userNotAuthenticatedEvent

    fun saveGender(gender: String) {
        val currentUser = repository.getCurrentUser()

        if (currentUser == null) {
            _userNotAuthenticatedEvent.value = true
            return
        }

        val userId = currentUser.uid

        // Check if user document exists first
        repository.getUserData(userId).addOnSuccessListener { document ->
            if (document.exists()) {
                // Edit mode - only update the gender field
                repository.updateUserField(userId, "gender", gender)
                    .addOnSuccessListener {
                        Log.d(TAG, "Gender updated successfully")
                        _saveSuccessEvent.value = true
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating gender: ${e.message}")
                        _errorEvent.value = "Error updating gender. Please try again"
                    }
            } else {
                // New user - create initial profile with minimal data
                // for first-time user setup
                val userData = HashMap<String, Any>()
                userData["gender"] = gender
                userData["bookings"] = ArrayList<Any>()
                userData["sports_liked"] = ArrayList<Any>()
                userData["venues_liked"] = ArrayList<Any>()
                // Use empty string instead of null for Firestore compatibility
                userData["birth_date"] = ""

                // Add name if available
                currentUser.displayName?.let {
                    userData["name"] = it
                } ?: run {
                    userData["name"] = ""  // If no name, save an empty string
                }

                // Add email as additional data if available
                currentUser.email?.let {
                    userData["email"] = it
                }

                // Save to Firestore - only for new users
                repository.createUserProfile(userId, userData)
                    .addOnSuccessListener {
                        Log.d(TAG, "User data saved successfully to Firestore")
                        _saveSuccessEvent.value = true
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving user data to Firestore: ${e.message}")
                        _errorEvent.value = "Error saving data. Please try again"
                    }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error checking if user exists: ${e.message}")
            _errorEvent.value = "Error checking user data. Please try again"
        }
    }

    fun checkAuthentication(): Boolean {
        return repository.getCurrentUser() != null
    }
}