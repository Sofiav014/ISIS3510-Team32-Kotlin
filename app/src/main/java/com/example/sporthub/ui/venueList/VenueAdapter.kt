package com.example.sporthub.ui.venueList.adapter

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.data.model.Venue

class VenueAdapter : RecyclerView.Adapter<VenueAdapter.VenueViewHolder>() {

    private var venues: List<Venue> = emptyList()
    private var userLocation: Location? = null

    class VenueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val venueImage: ImageView = view.findViewById(R.id.venueImage)
        val venueName: TextView = view.findViewById(R.id.venueName)
        val venueLocation: TextView = view.findViewById(R.id.venueLocation)
        val venueSport: TextView = view.findViewById(R.id.venueSport)
        val venueRating: TextView = view.findViewById(R.id.venueRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VenueViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_venue_card, parent, false)
        return VenueViewHolder(view)
    }

    override fun onBindViewHolder(holder: VenueViewHolder, position: Int) {
        val venue = venues[position]

        // Set venue name
        holder.venueName.text = venue.name

        // Set venue location - MOSTRAR LA UBICACIÓN EN SU CAMPO ORIGINAL
        holder.venueLocation.text = venue.name

        // Calculate and display distance in the sport text view
        if (userLocation != null && venue.coords != null) {
            val venueLocation = Location("").apply {
                latitude = venue.coords.latitude
                longitude = venue.coords.longitude
            }
            val distanceInMeters = userLocation!!.distanceTo(venueLocation)
            val distanceText = String.format("%.1f km", distanceInMeters / 1000)

            // Display distance in sport field
            holder.venueSport.text = "Distance: $distanceText"
        } else {
            // If no location is available, show sport name as fallback
            holder.venueSport.text = venue.sport?.name ?: "Unknown Sport"
        }

        // Set rating
        holder.venueRating.text = String.format("%.1f", venue.rating.toFloat())

        // Load image
        Glide.with(holder.itemView.context)
            .load(venue.image)
            .into(holder.venueImage)
    }

    override fun getItemCount() = venues.size

    fun submitList(newVenues: List<Venue>) {
        venues = newVenues
        notifyDataSetChanged()
    }

    fun setUserLocation(location: Location?) {
        userLocation = location
        notifyDataSetChanged() // Refresh to update distances
    }
}