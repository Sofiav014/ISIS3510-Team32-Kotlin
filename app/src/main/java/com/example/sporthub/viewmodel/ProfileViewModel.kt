package com.example.sporthub.ui.profile

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.model.User
import com.example.sporthub.data.model.Venue
import com.example.sporthub.data.repository.UserRepository
import com.example.sporthub.utils.ThemeManager
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository()
    private val themeManager = ThemeManager.getInstance(application)

    private val _userData = MutableLiveData<User>()
    val userData: LiveData<User> = _userData

    private val _favoriteVenues = MutableLiveData<List<Venue>>()
    val favoriteVenues: LiveData<List<Venue>> = _favoriteVenues

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _isDarkMode = MutableLiveData<Boolean>()
    val isDarkMode: LiveData<Boolean> = _isDarkMode

    init {
        _isDarkMode.value = themeManager.isDarkModeActive()
    }

    fun loadUserData() {
        _isLoading.value = true

        val currentUser = userRepository.getCurrentUser()
        if (currentUser != null) {
            userRepository.getUserModel(currentUser.uid).observeForever { user ->
                _userData.value = user
                _favoriteVenues.value = user.venuesLiked
                _isLoading.value = false
            }
        } else {
            _errorMessage.value = "User not authenticated"
            _isLoading.value = false
        }
    }

    // Format birth date for display
    fun formatBirthDate(timestamp: Timestamp?): String {
        return if (timestamp != null) {
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            dateFormat.format(timestamp.toDate())
        } else {
            "Not specified"
        }
    }

    // Helper method to get user's age
    @RequiresApi(Build.VERSION_CODES.O)
    fun calculateAge(birthDate: Timestamp?): Int {
        if (birthDate == null) return 0

        val birthLocalDate = birthDate.toDate().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val currentDate = LocalDate.now()

        return currentDate.year - birthLocalDate.year -
                if (currentDate.monthValue < birthLocalDate.monthValue ||
                    (currentDate.monthValue == birthLocalDate.monthValue &&
                            currentDate.dayOfMonth < birthLocalDate.dayOfMonth)) 1 else 0
    }

    // Theme management methods
    fun isDarkModeActive(): Boolean {
        return themeManager.isDarkModeActive()
    }

    fun toggleDarkMode() {
        themeManager.toggleDarkMode()
        _isDarkMode.value = themeManager.isDarkModeActive()
    }

    // Sign out method
    fun signOut() {
        userRepository.signOut()
    }
}