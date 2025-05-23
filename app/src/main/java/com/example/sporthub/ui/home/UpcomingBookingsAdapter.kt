package com.example.sporthub.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.example.sporthub.data.model.Booking
import com.example.sporthub.ui.home.viewholder.UpcomingBookingViewHolder
import com.example.sporthub.viewmodel.HomeViewModel
import android.util.Log

class UpcomingBookingsAdapter(private val homeViewModel: HomeViewModel) : ListAdapter<Booking, UpcomingBookingViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpcomingBookingViewHolder {
        return UpcomingBookingViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: UpcomingBookingViewHolder, position: Int) {
        val booking = getItem(position)
        Log.d("UpcomingBooking:333", "Binding booking: $booking")
        Log.d("UpcomingBookingAdapter", "onBindViewHolder - binding booking at position $position: $booking")
        holder.bind(booking, homeViewModel)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Booking>() {
            override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean = oldItem == newItem
        }
    }
}
