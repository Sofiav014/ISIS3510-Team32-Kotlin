package com.example.sporthub.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.sporthub.data.db.AppDatabase
import com.example.sporthub.data.model.Venue
import com.example.sporthub.data.repository.FavoriteVenueRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FavoriteVenuesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FavoriteVenueRepository
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    // LiveData for favorite venues
    val favoriteVenues: LiveData<List<Venue>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FavoriteVenueRepository(database.favoriteVenueDao(), application)

        // Initialize favorite venues LiveData
        favoriteVenues = if (userId != null) {
            repository.getFavoriteVenues(userId).asLiveData()
        } else {
            MutableLiveData(emptyList())
        }
    }

    // Add venue to favorites
    fun addToFavorites(venue: Venue) {
        userId?.let { id ->
            viewModelScope.launch {
                repository.addToFavorites(venue, id)
            }
        }
    }

    // Remove venue from favorites
    fun removeFromFavorites(venueId: String) {
        userId?.let { id ->
            viewModelScope.launch {
                repository.removeFromFavorites(venueId, id)
            }
        }
    }

    // Check if venue is in favorites
    suspend fun isVenueFavorite(venueId: String): Boolean {
        return userId?.let { id ->
            repository.isVenueFavorite(venueId, id)
        } ?: false
    }

    // Sync with remote database
    fun syncWithRemote() {
        userId?.let { id ->
            viewModelScope.launch {
                repository.syncWithRemote(id)
            }
        }
    }
}