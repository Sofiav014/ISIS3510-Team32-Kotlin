package com.example.sporthub.ui.profile

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.sporthub.data.model.User
import com.example.sporthub.data.model.Venue
import com.example.sporthub.data.repository.UserRepository
import com.example.sporthub.ui.login.SignInActivity
import com.example.sporthub.utils.LocalThemeManager
import com.example.sporthub.utils.ThemeManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository()
    private val themeManager = ThemeManager.getInstance(application)
    private val firestore = FirebaseFirestore.getInstance()

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

    private val _profilePictureUrl = MutableLiveData<String?>()
    val profilePictureUrl: LiveData<String?> = _profilePictureUrl

    init {
        // Initialize with the current system theme status
        updateThemeStatus()
    }

    private var currentUserObserver: Observer<User>? = null

    fun loadUserData() {
        val currentUser = userRepository.getCurrentUser()
        if (currentUser == null) {
            _errorMessage.value = "User not authenticated"
            _isLoading.value = false
            return
        }

        // Si ya tenemos datos, no recargar (salvo que quieras forzar recarga)
        if (_userData.value != null) {
            Log.d("ProfileViewModel", "User already loaded, skipping fetch")
            return
        }

        _isLoading.value = true
        Log.d("ProfileViewModel", "Fetching user data for ${currentUser.uid}")

        currentUserObserver?.let {
            userRepository.getUserModel(currentUser.uid).removeObserver(it)
        }

        val observer = Observer<User> { user ->
            Log.d("ProfileViewModel", "User data received: ${user.name}")
            _userData.value = user
            _favoriteVenues.value = user.venuesLiked
            _isLoading.value = false
            updateThemeStatus()
        }

        currentUserObserver = observer
        userRepository.getUserModel(currentUser.uid).observeForever(observer)
    }

    fun loadProfilePicture(userId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    val profilePictureUrl = userDoc.getString("profile_picture_url")

                    withContext(Dispatchers.Main) {
                        _profilePictureUrl.value = profilePictureUrl
                        Log.d("ProfileViewModel", "Profile picture loaded: $profilePictureUrl")
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile picture: ${e.message}")
                withContext(Dispatchers.Main) {
                    _profilePictureUrl.value = null
                }
            }
        }
    }

    fun updateProfilePicture(userId: String, imageUrl: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    firestore.collection("users")
                        .document(userId)
                        .update("profile_picture_url", imageUrl)
                        .await()
                }

                // Update local state immediately
                withContext(Dispatchers.Main) {
                    _profilePictureUrl.value = imageUrl
                    Log.d("ProfileViewModel", "Profile picture updated successfully: $imageUrl")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error updating profile picture: ${e.message}")
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to update profile picture: ${e.message}"
                }
            }
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
        try {
            val appContext = getApplication<Application>()
            val prefs = appContext.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

            // 1. Marcar que el cambio de tema está en curso
            prefs.edit().putBoolean("is_theme_changing", true).commit()

            // 2. Determinar el nuevo modo de tema
            Log.d("ProfileViewModel", "Toggling dark mode. Current: ${isDarkModeActive()}")
            val newDarkModeValue = !isDarkModeActive()
            _isDarkMode.value = newDarkModeValue

            // 3. Aplicar el nuevo tema inmediatamente (NO depende de red ni usuario)
            val mode = if (newDarkModeValue) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)

            // 4. Guardar la preferencia de forma asíncrona, pero NO bloquear el cambio
            val userId = userRepository.getCurrentUser()?.uid
            if (userId != null) {
                // Usamos apply() porque no necesitamos persistencia inmediata
                appContext.getSharedPreferences("user_theme_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("theme_for_user_$userId", newDarkModeValue)
                    .apply()
            }

            // 5. Limpiar el flag de transición después de un tiempo suficiente
            Handler(Looper.getMainLooper()).postDelayed({
                prefs.edit().putBoolean("is_theme_changing", false).apply()
            }, 1500) // Aumentado a 1500ms para evitar problemas si el sistema va lento

        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error in toggleDarkMode: ${e.message}")
        }
    }

    fun Context.isThemeChanging(): Boolean {
        return try {
            getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
                .getBoolean("is_theme_changing", false)
        } catch (e: Exception) {
            false
        }
    }

    fun signOut() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val userId = userRepository.getCurrentUser()?.uid
        userRepository.signOut()
        SignInActivity.preferencesAlreadyChecked = false
    }
}