package com.example.sporthub.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.example.sporthub.data.model.Booking
import com.example.sporthub.ui.home.viewholder.RecommendedBookingViewHolder
import com.example.sporthub.viewmodel.HomeViewModel


class RecommendedBookingsAdapter(private val homeViewModel: HomeViewModel, private val onBookingClick: (Booking) -> Unit ) : ListAdapter<Booking, RecommendedBookingViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendedBookingViewHolder {
        return RecommendedBookingViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: RecommendedBookingViewHolder, position: Int) {
        val booking = getItem(position)
        holder.bind(booking, homeViewModel)

        holder.itemView.setOnClickListener {
            onBookingClick(booking)  // Trigger the callback when the item is clicked
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Booking>() {
            override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean = oldItem == newItem
        }
    }
}
