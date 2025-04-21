package com.example.sporthub.ui.venueList.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.data.model.Venue
import java.util.Locale

class VenueAdapter : RecyclerView.Adapter<VenueAdapter.VenueViewHolder>() {

    private var venues: List<Venue> = emptyList()

    class VenueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val venueImage: ImageView = view.findViewById(R.id.venueImage)
        val venueName: TextView = view.findViewById(R.id.venueName)
        val venueLocation: TextView = view.findViewById(R.id.venueLocation)
        val venueSport: TextView = view.findViewById(R.id.venueSport)
        val venueRating: TextView = view.findViewById(R.id.venueRating) // ✅ Changed from RatingBar to TextView

        val venueDistance: TextView = view.findViewById(R.id.venueDistance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VenueViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_venue_card, parent, false)
        return VenueViewHolder(view)
    }

    override fun onBindViewHolder(holder: VenueViewHolder, position: Int) {
        val venue = venues[position]
        holder.venueName.text = venue.name
        holder.venueLocation.text = venue.name
        holder.venueSport.text = venue.sport?.name
        holder.venueRating.text = String.format("%.1f", venue.rating.toFloat())
        Glide.with(holder.itemView.context)
            .load(venue.image)
            .into(holder.venueImage)

        // Display the distance in the venue cards on the Venue List
        val distanceKm = venue.distanceInKm
        if (distanceKm != null) {
            holder.venueDistance.visibility = View.VISIBLE
            holder.venueDistance.text = String.format(Locale.getDefault(), "%.2f km away", distanceKm)
        } else {
            holder.venueDistance.visibility = View.GONE
        }

    }

    override fun getItemCount() = venues.size

    fun submitList(newVenues: List<Venue>) {
        venues = newVenues
        notifyDataSetChanged()
    }
}
