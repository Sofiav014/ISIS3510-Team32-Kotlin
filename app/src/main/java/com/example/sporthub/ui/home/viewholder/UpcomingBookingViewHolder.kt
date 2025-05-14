package com.example.sporthub.ui.home.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.data.model.Booking
import com.example.sporthub.databinding.ItemUpcomingBookingBinding
import com.example.sporthub.viewmodel.HomeViewModel


class UpcomingBookingViewHolder(private val binding: ItemUpcomingBookingBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(booking: Booking, homeViewModel: HomeViewModel) {
        binding.textVenueName.text = booking.venue?.name ?: "Unknown Venue"
        binding.textSport.text = booking.venue?.sport?.name ?: "Unknown Sport"
        binding.textRating.text = booking.venue?.rating.toString()
        val imageUrl = booking.venue?.image ?: ""
        homeViewModel.loadImageIntoImageView(binding.root.context, imageUrl, binding.imageBackground)
    }

    companion object {
        fun create(parent: ViewGroup): UpcomingBookingViewHolder {
            val binding = ItemUpcomingBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return UpcomingBookingViewHolder(binding)
        }
    }
}
