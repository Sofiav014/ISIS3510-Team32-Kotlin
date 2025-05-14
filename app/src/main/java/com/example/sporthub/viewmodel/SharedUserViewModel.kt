// SharedUserViewModel.kt
package com.example.sporthub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.model.User

class SharedUserViewModel : ViewModel() {
    private val _currentUser = MutableLiveData<User>() // Mutable data
    val currentUser: LiveData<User> = _currentUser

    // Method to set a new user
    fun setUser(user: User) {
        _currentUser.value = user
    }

    // Method to update the user
    fun updateCurrentUser(user: User) {
        _currentUser.postValue(user)
    }
}
