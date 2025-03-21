package com.example.sporthub.ui.home.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.data.model.Booking
import com.example.sporthub.databinding.ItemUpcomingBookingBinding


class UpcomingBookingViewHolder(private val binding: ItemUpcomingBookingBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(booking: Booking) {
        binding.textVenueName.text = booking.venue?.name ?: "Unknown Venue"
        binding.textSport.text = booking.venue?.sport?.name ?: "Unknown Sport"
        binding.textRating.text = booking.venue?.rating.toString()
        val imageUrl = booking.venue?.imageUrl ?: ""
        Glide.with(binding.root.context)
            .load(imageUrl)
            .into(binding.imageBackground) // Set to ImageView
    }

    companion object {
        fun create(parent: ViewGroup): UpcomingBookingViewHolder {
            val binding = ItemUpcomingBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return UpcomingBookingViewHolder(binding)
        }
    }
}
