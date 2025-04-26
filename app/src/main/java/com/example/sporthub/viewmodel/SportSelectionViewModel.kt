package com.example.sporthub.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.repository.UserRepository

class SportSelectionViewModel : ViewModel() {
    private val repository = UserRepository()
    private val TAG = "SportSelectionViewModel"

    private val _saveSuccessEvent = MutableLiveData<Boolean>()
    val saveSuccessEvent: LiveData<Boolean> = _saveSuccessEvent

    private val _errorEvent = MutableLiveData<String>()
    val errorEvent: LiveData<String> = _errorEvent

    private val _userNotAuthenticatedEvent = MutableLiveData<Boolean>()
    val userNotAuthenticatedEvent: LiveData<Boolean> = _userNotAuthenticatedEvent

    // List of available sports with their data
    val sportsData = mapOf(
        "Basketball" to mapOf(
            "id" to "basketball",
            "name" to "Basketball",
            "logo" to "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Fbasketball-logo.png?alt=media&token=fa52fa07-44ea-4465-b33b-cb07fa2fb228"
        ),
        "Football" to mapOf(
            "id" to "football",
            "name" to "Football",
            "logo" to "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Ffootball-logo.png?alt=media&token=3c8d8b50-b926-4a0a-8b7b-224a8e3b352c"
        ),
        "Volleyball" to mapOf(
            "id" to "volleyball",
            "name" to "Volleyball",
            "logo" to "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Fvolleyball-logo.png?alt=media&token=b51de9d4-f1b4-4ede-a3a0-5777523b2cb9"
        ),
        "Tennis" to mapOf(
            "id" to "tennis",
            "name" to "Tennis",
            "logo" to "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Ftennis-logo.png?alt=media&token=84fde031-9c77-4cc5-b4d3-dd785e203b99"
        )
    )

    fun saveSportsPreferences(selectedSportsKeys: List<String>) {
        val currentUser = repository.getCurrentUser()

        if (currentUser == null) {
            _userNotAuthenticatedEvent.value = true
            return
        }

        // Verify that at least one sport is selected
        if (selectedSportsKeys.isEmpty()) {
            _errorEvent.value = "Please select at least one sport to continue"
            return
        }

        val userId = currentUser.uid

        // Create list of selected sports with their complete data
        val sportsList = selectedSportsKeys.map { sportName ->
            sportsData[sportName]
        }

        // Only update the sports_liked field
        repository.updateUserSports(userId, sportsList)
            .addOnSuccessListener {
                Log.d(TAG, "Sports preferences saved successfully")
                _saveSuccessEvent.value = true
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving sports preferences: ${e.message}")
                _errorEvent.value = "Error saving data. Please try again"
            }
    }

    fun checkAuthentication(): Boolean {
        return repository.getCurrentUser() != null
    }
}