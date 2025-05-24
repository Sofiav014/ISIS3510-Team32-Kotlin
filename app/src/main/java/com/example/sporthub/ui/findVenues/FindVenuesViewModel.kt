package com.example.sporthub.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sporthub.data.model.CachedVenue
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.Venue
import com.example.sporthub.utils.ImageUrlStore
import com.example.sporthub.utils.LRUCache
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File

class FindVenuesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _venues = MutableLiveData<List<Venue>>()
    val venues: LiveData<List<Venue>> get() = _venues

    val venueCache = LRUCache<String, List<CachedVenue>>(maxSize = 20)


    // Lista de deportes disponibles
    val sportsList = listOf(
        Sport(id = "basketball", name = "Basketball", logo = "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Fbasketball-logo.png?alt=media&token=fa52fa07-44ea-4465-b33b-cb07fa2fb228"),
        Sport(id = "football", name = "Football", logo = "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Ffootball-logo.png?alt=media&token=3c8d8b50-b926-4a0a-8b7b-224a8e3b352c"),
        Sport(id = "volleyball", name = "Volleyball", logo = "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Fvolleyball-logo.png?alt=media&token=b51de9d4-f1b4-4ede-a3a0-5777523b2cb9"),
        Sport(id = "tennis", name = "Tennis", logo = "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Ftennis-logo.png?alt=media&token=84fde031-9c77-4cc5-b4d3-dd785e203b99")
    )

    private fun saveImageToInternalStorage(context: Context, url: String, filename: String): String? {
        return try {
            val input = java.net.URL(url).openStream()
            val file = File(context.filesDir, filename)
            file.outputStream().use { input.copyTo(it) }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun deleteUnusedVenueImages(context: Context, activeVenueIds: Set<String>) {
        val filesDir = context.filesDir
        filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("venue_") && file.name.endsWith(".jpg")) {
                val id = file.name.removePrefix("venue_").removeSuffix(".jpg")
                if (id !in activeVenueIds) {
                    file.delete()
                }
            }
        }
    }



    suspend fun fetchVenuesBySport(
        sportId: String,
        forceFetchFromNetwork: Boolean = false,
        appContext: Context
    ) {
        val cachedVenues = venueCache[sportId]

        if (!forceFetchFromNetwork && !cachedVenues.isNullOrEmpty()) {
            withContext(Dispatchers.Main) {
                _venues.value = cachedVenues.map { cached ->
                    Venue(
                        id = cached.id,
                        coords = cached.coords,
                        image = "",
                        locationName = cached.locationName,
                        name = cached.name,
                        rating = cached.rating,
                        sport = Sport(id = cached.sportId, name = "", logo = ""),
                        bookings = null
                    )
                }
            }
            return
        }

        try {
            val venueList = withContext(Dispatchers.IO) {
                val snapshot = db.collection("venues")
                    .whereEqualTo("sport.id", sportId)
                    .get()
                    .await()

                val rawList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Venue::class.java)?.copy(id = doc.id)
                }

                val processedList = rawList.map { venue ->
                    val filename = "venue_${venue.id}.jpg"
                    val imageFile = File(appContext.filesDir, filename)

                    val imagePath = if (imageFile.exists()) {
                        imageFile.absolutePath
                    } else {
                        val path = saveImageToInternalStorage(appContext, venue.image, filename)
                        ImageUrlStore.saveImageUrl(appContext, venue.id, venue.image)
                        path ?: ""
                    }

                    venue.copy(image = imagePath)
                }

                deleteUnusedVenueImages(appContext, processedList.map { it.id }.toSet())
                processedList
            }

            withContext(Dispatchers.Main) {
                _venues.value = venueList

                val cachedList = venueList.map { venue ->
                    CachedVenue(
                        id = venue.id,
                        coords = venue.coords,
                        locationName = venue.locationName,
                        name = venue.name,
                        rating = venue.rating,
                        sportId = venue.sport?.id ?: ""
                    )
                }

                venueCache[sportId] = cachedList
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                if (!cachedVenues.isNullOrEmpty()) {
                    _venues.value = cachedVenues.map { cached ->
                        Venue(
                            id = cached.id,
                            coords = cached.coords,
                            image = "",
                            locationName = cached.locationName,
                            name = cached.name,
                            rating = cached.rating,
                            sport = Sport(id = cached.sportId, name = "", logo = ""),
                            bookings = null
                        )
                    }
                } else {
                    _venues.value = emptyList()
                }
            }
        }
    }


}