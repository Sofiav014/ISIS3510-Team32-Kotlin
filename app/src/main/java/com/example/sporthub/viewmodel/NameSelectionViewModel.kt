// com.example.sporthub.viewmodel.NameSelectionViewModel.kt
package com.example.sporthub.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.repository.UserRepository
import com.google.firebase.auth.UserProfileChangeRequest
import java.util.ArrayList

class NameSelectionViewModel : ViewModel() {
    private val repository = UserRepository()
    private val TAG = "NameSelectionViewModel"

    private val _saveSuccessEvent = MutableLiveData<Boolean>()
    val saveSuccessEvent: LiveData<Boolean> = _saveSuccessEvent

    private val _errorEvent = MutableLiveData<String>()
    val errorEvent: LiveData<String> = _errorEvent

    private val _userNotAuthenticatedEvent = MutableLiveData<Boolean>()
    val userNotAuthenticatedEvent: LiveData<Boolean> = _userNotAuthenticatedEvent

    fun saveName(name: String) {
        val currentUser = repository.getCurrentUser()

        if (currentUser == null) {
            _userNotAuthenticatedEvent.value = true
            return
        }

        // Actualizar el perfil de autenticación
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()

        currentUser.updateProfile(profileUpdates)
            .addOnSuccessListener {
                Log.d(TAG, "User profile updated with new name")

                // Después de actualizar el perfil, verificar si el usuario ya existe en Firestore
                repository.getUserData(currentUser.uid)
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // Si el documento existe, actualizar solo el nombre
                            repository.updateUserName(currentUser.uid, name)
                                .addOnSuccessListener {
                                    Log.d(TAG, "User name updated in Firestore")
                                    _saveSuccessEvent.value = true
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error updating user name in Firestore: ${e.message}")
                                    _errorEvent.value = "Error saving data. Please try again"
                                }
                        } else {
                            // Si el documento no existe, crear un nuevo perfil de usuario
                            // Eliminamos la línea que guarda el email
                            val userData = hashMapOf<String, Any>(
                                "name" to name,
                                // Ya no guardamos el email aquí
                                "bookings" to ArrayList<String>(),
                                "sports_liked" to ArrayList<String>(),
                                "venues_liked" to ArrayList<String>(),
                                "birth_date" to "",
                                "gender" to ""
                            )

                            repository.createUserProfile(currentUser.uid, userData)
                                .addOnSuccessListener {
                                    Log.d(TAG, "User document created in Firestore")
                                    _saveSuccessEvent.value = true
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error creating user document: ${e.message}")
                                    _errorEvent.value = "Error creating profile. Please try again"
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error checking if user document exists: ${e.message}")
                        _errorEvent.value = "Error checking profile. Please try again"
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating user profile: ${e.message}")
                _errorEvent.value = "Error updating profile. Please try again"
            }
    }

    fun checkAuthentication(): Boolean {
        return repository.getCurrentUser() != null
    }
}