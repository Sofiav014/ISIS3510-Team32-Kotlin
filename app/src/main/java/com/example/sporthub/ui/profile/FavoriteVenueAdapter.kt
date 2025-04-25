package com.example.sporthub.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.data.model.Venue

class FavoriteVenueAdapter : RecyclerView.Adapter<FavoriteVenueAdapter.VenueViewHolder>() {

    private var venues: List<Venue> = emptyList()

    class VenueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val venueImage: ImageView = view.findViewById(R.id.venueImage)
        val venueName: TextView = view.findViewById(R.id.venueName)
        val venueLocation: TextView = view.findViewById(R.id.venueLocation)
        val venueSport: TextView = view.findViewById(R.id.venueSport)
        val venueRating: TextView = view.findViewById(R.id.venueRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VenueViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_venue, parent, false)
        return VenueViewHolder(view)
    }

    override fun onBindViewHolder(holder: VenueViewHolder, position: Int) {
        val venue = venues[position]

        // Set venue name
        holder.venueName.text = venue.name

        // Set venue location
        holder.venueLocation.text = venue.locationName

        // Set sport name if available
        venue.sport?.let {
            holder.venueSport.text = it.name
        } ?: run {
            holder.venueSport.text = "Unknown Sport"
        }

        // Set rating
        holder.venueRating.text = String.format("%.1f", venue.rating)

        // Load image with Glide
        Glide.with(holder.itemView.context)
            .load(venue.image)
            .placeholder(R.drawable.ic_sport_venue_card)
            .into(holder.venueImage)
    }

    override fun getItemCount() = venues.size

    fun submitList(venuesList: List<Venue>) {
        venues = venuesList
        notifyDataSetChanged()
    }
}