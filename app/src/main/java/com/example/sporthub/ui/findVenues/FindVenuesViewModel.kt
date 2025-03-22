package com.example.sporthub.ui.findVenues

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.Venue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class FindVenuesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _venues = MutableLiveData<List<Venue>>()
    val venues: LiveData<List<Venue>> get() = _venues

    // Lista de deportes disponibles

    val sportsList = listOf(
        Sport(id = "basketball", name = "Basketball", logo = "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Fbasketball-logo.png?alt=media&token=fa52fa07-44ea-4465-b33b-cb07fa2fb228"),
        Sport(id = "football", name = "Football", logo = "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Ffootball-logo.png?alt=media&token=3c8d8b50-b926-4a0a-8b7b-224a8e3b352c"),
        Sport(id = "volleyball", name = "Volleyball", logo = "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Fvolleyball-logo.png?alt=media&token=b51de9d4-f1b4-4ede-a3a0-5777523b2cb9"),
        Sport(id = "tennis", name = "Tennis", logo = "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Ftennis-logo.png?alt=media&token=84fde031-9c77-4cc5-b4d3-dd785e203b99")
    )

    fun fetchVenuesBySport(sportId: String) {
        db.collection("venues")
            .whereEqualTo("sport.id", sportId)  // Filter by selected sport
            .get()
            .addOnSuccessListener { snapshot: QuerySnapshot ->
                val venueList = snapshot.documents.mapNotNull { it.toObject(Venue::class.java) }
                _venues.value = venueList
            }
            .addOnFailureListener {
                _venues.value = emptyList()  // Handle failure
            }
    }

}