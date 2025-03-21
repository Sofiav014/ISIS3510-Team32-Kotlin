// com.example.sporthub.viewmodel.GenderSelectionViewModel.kt
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

        // Crear un objeto con los datos del usuario
        val userData = hashMapOf(
            "gender" to gender,
            "bookings" to ArrayList<String>(),  // Lista vacía para bookings
            "sports_liked" to ArrayList<String>(),  // Lista vacía para deportes preferidos
            "venues_liked" to ArrayList<String>(),  // Lista vacía para lugares preferidos
            "birth_date" to ""  // Fecha de nacimiento vacía por ahora
        )

        // Añadir nombre si está disponible
        currentUser.displayName?.let {
            userData["name"] = it
        } ?: run {
            userData["name"] = ""  // Si no hay nombre, guardar un string vacío
        }

        // Añadir email como dato adicional si está disponible
        currentUser.email?.let {
            userData["email"] = it
        }

        // Guardar en Firestore
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

    fun checkAuthentication(): Boolean {
        return repository.getCurrentUser() != null
    }
}