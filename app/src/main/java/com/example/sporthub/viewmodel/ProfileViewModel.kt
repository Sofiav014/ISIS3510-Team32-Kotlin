package com.example.sporthub.ui.profile

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.model.User
import com.example.sporthub.data.model.Venue
import com.example.sporthub.data.repository.UserRepository
import com.example.sporthub.ui.login.SignInActivity
import com.example.sporthub.utils.LocalThemeManager
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
        // Initialize with the current system theme status
        updateThemeStatus()
    }

    fun loadUserData() {
        _isLoading.value = true

        val currentUser = userRepository.getCurrentUser()
        if (currentUser != null) {
            userRepository.getUserModel(currentUser.uid).observeForever { user ->
                _userData.value = user
                _favoriteVenues.value = user.venuesLiked
                _isLoading.value = false

                // Update theme status based on current user preference
                updateThemeStatus()
            }
        } else {
            _errorMessage.value = "User not authenticated"
            _isLoading.value = false
        }
    }

    fun getCurrentUserId(): String? {
        return userRepository.getCurrentUser()?.uid
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
        return AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
    }

    // Update theme status based on current settings
    private fun updateThemeStatus() {
        val userId = userRepository.getCurrentUser()?.uid
        if (userId != null) {
            // Get user's saved preference
            val savedDarkMode = LocalThemeManager.getUserTheme(getApplication(), userId)

            // Fixed: Don't directly assign potentially nullable savedDarkMode to _isDarkMode
            // Instead, use a safe default value (current theme state) if savedDarkMode is null
            _isDarkMode.value = savedDarkMode ?: isDarkModeActive()
        } else {
            // If no user, just use current theme state
            _isDarkMode.value = isDarkModeActive()
        }
    }

    fun toggleDarkMode() {
        // Check connectivity first
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isOffline = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected != true
        }

        // Log connectivity status
        Log.d("ThemeSwitch", "Network status: ${if (isOffline) "Offline" else "Online"}")

        // Save the flag to prevent activities from recreating improperly
        val editor = getApplication<Application>().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE).edit()
        editor.putBoolean("is_theme_changing", true)
        editor.putBoolean("is_offline_theme_change", isOffline)
        editor.apply()

        // Get current theme status and toggle it
        val newDarkModeValue = !isDarkModeActive()

        // Update our LiveData before changing the theme
        _isDarkMode.value = newDarkModeValue

        // Save user preference LOCALLY WITHOUT NETWORK OPERATIONS if offline
        val userId = userRepository.getCurrentUser()?.uid
        if (userId != null) {
            // Use a simpler local-only save when offline
            if (isOffline) {
                // Skip any potential network operations
                val sharedPrefs = getApplication<Application>().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit()
                    .putBoolean("theme_${userId}", newDarkModeValue)
                    .apply()
            } else {
                // Normal save when online
                LocalThemeManager.saveUserTheme(getApplication(), userId, newDarkModeValue)
            }
        }

        // Apply the theme change immediately without delay
        val mode = if (newDarkModeValue) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)

        // Clear the flag after a short delay to ensure it's processed
        Handler(Looper.getMainLooper()).postDelayed({
            val editorCleanup = getApplication<Application>().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE).edit()
            editorCleanup.putBoolean("is_theme_changing", false)
            editorCleanup.putBoolean("is_offline_theme_change", false)
            editorCleanup.apply()
        }, 1000) // Give it a full second to complete the transition
    }

    fun signOut() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val userId = userRepository.getCurrentUser()?.uid
        userRepository.signOut()
        SignInActivity.preferencesAlreadyChecked = false
    }
}