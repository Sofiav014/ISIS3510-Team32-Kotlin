package com.example.sporthub.ui.home.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sporthub.data.model.Booking
import com.example.sporthub.databinding.ItemRecommendedBookingBinding
import java.text.SimpleDateFormat
import java.util.*


class RecommendedBookingViewHolder(private val binding: ItemRecommendedBookingBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(booking: Booking) {

        val startDate = booking.startTime?.toDate()
        val dateFormatter = SimpleDateFormat("MMMM dd", Locale.getDefault())
        val formattedDate = dateFormatter.format(startDate)
        binding.textDate.text = formattedDate

        // Format the time and calculate duration
        val startTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedStartTime = startTimeFormatter.format(startDate)

        val endTime = booking.endTime?.toDate()
        val durationMinutes = if (startDate != null && endTime != null) {
            (endTime.time - startDate.time) / (1000 * 60) // Calculate minutes
        } else {
            0
        }

        binding.textTime.text = "$formattedStartTime h ($durationMinutes min)"

        binding.textUsers.text = "${booking.users.size} / ${booking.maxUsers}"
        val imageUrl = booking.venue?.image ?: ""  // Replace with actual property
        Glide.with(binding.root.context)
            .load(imageUrl)
            .into(binding.imageBackground) // Set to ImageView
    }

    companion object {
        fun create(parent: ViewGroup): RecommendedBookingViewHolder {
            val binding = ItemRecommendedBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return RecommendedBookingViewHolder(binding)
        }
    }
}
