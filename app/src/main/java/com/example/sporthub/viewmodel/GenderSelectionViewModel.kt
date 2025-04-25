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
        val isEditMode = repository.getUserData(userId).isComplete

        if (isEditMode) {
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
            // New user registration - create complete profile
            // Create an object with the user data
            val userData = hashMapOf(
                "gender" to gender,
                "bookings" to ArrayList<String>(),  // Empty list for bookings
                "sports_liked" to ArrayList<String>(),  // Empty list for favorite sports
                "venues_liked" to ArrayList<String>(),  // Empty list for favorite venues
                "birth_date" to null  // Empty birth date for now
            )

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

            // Save to Firestore
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
    }

    fun checkAuthentication(): Boolean {
        return repository.getCurrentUser() != null
    }
}