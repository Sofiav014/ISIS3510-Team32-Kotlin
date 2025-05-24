// SharedUserViewModel.kt
package com.example.sporthub.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.model.User
import com.example.sporthub.data.repository.UserRepository

class SharedUserViewModel : ViewModel() {
    private val _currentUser = MutableLiveData<User>()
    val currentUser: LiveData<User> = _currentUser

    private val userRepository = UserRepository()

    // Method to set a new user
    fun setUser(user: User) {
        _currentUser.value = user
    }

    // Method to update the user
    fun updateCurrentUser(user: User) {
        _currentUser.postValue(user)
    }

    // Method to refresh user data from Firebase
    fun refreshCurrentUser() {
        val firebaseUser = userRepository.getCurrentUser()
        if (firebaseUser != null) {
            Log.d("SharedUserViewModel", "Refreshing user data for ${firebaseUser.uid}")

            // Clear cache and force fresh fetch
            userRepository.clearUserCache()

            // Observe fresh data - but remove previous observer first to avoid memory leaks
            userRepository.getUserModel(firebaseUser.uid).observeForever { user ->
                if (user != null && user.id.isNotEmpty()) {
                    Log.d("SharedUserViewModel", "Fresh user data loaded: ${user.name}")
                    _currentUser.value = user
                }
            }
        } else {
            Log.w("SharedUserViewModel", "No authenticated user found for refresh")
        }
    }
}