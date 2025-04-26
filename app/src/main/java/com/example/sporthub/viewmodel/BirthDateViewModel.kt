package com.example.sporthub.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.repository.UserRepository
import com.google.firebase.Timestamp
import java.util.Date

class BirthDateViewModel : ViewModel() {
    private val repository = UserRepository()
    private val TAG = "BirthDateViewModel"

    private val _saveSuccessEvent = MutableLiveData<Boolean>()
    val saveSuccessEvent: LiveData<Boolean> = _saveSuccessEvent

    private val _errorEvent = MutableLiveData<String>()
    val errorEvent: LiveData<String> = _errorEvent

    private val _userNotAuthenticatedEvent = MutableLiveData<Boolean>()
    val userNotAuthenticatedEvent: LiveData<Boolean> = _userNotAuthenticatedEvent

    fun saveBirthDate(birthDate: Date) {
        val currentUser = repository.getCurrentUser()

        if (currentUser == null) {
            _userNotAuthenticatedEvent.value = true
            return
        }

        val userId = currentUser.uid
        val timestamp = Timestamp(birthDate)

        // Only update the birth_date field
        repository.updateUserBirthDate(userId, timestamp)
            .addOnSuccessListener {
                Log.d(TAG, "Birth date saved successfully as timestamp")
                _saveSuccessEvent.value = true
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving birth date: ${e.message}")
                _errorEvent.value = "Error saving data. Please try again"
            }
    }

    fun checkAuthentication(): Boolean {
        return repository.getCurrentUser() != null
    }
}