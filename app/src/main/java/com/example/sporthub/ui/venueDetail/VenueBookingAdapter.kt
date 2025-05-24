package com.example.sporthub.ui.venueDetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sporthub.R
import com.example.sporthub.data.model.Booking
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(private val venueName: String) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private var bookings: List<Booking> = emptyList()

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.bookingTitle)
        val location: TextView = itemView.findViewById(R.id.bookingLocation)
        val time: TextView = itemView.findViewById(R.id.bookingTime)
        val spots: TextView = itemView.findViewById(R.id.bookingSpots)
        val date: TextView = itemView.findViewById(R.id.bookingDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking_card, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]

        val start = booking.startTime?.toDate()
        val end = booking.endTime?.toDate()

        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

        holder.title.text = venueName
        holder.location.text = venueName
        holder.spots.text = "${booking.users.size} / ${booking.maxUsers}"

        val startText = start?.let { timeFormatter.format(it) } ?: "N/A"
        val endText = end?.let { timeFormatter.format(it) } ?: "N/A"
        val dateText = start?.let { dateFormatter.format(it) } ?: "Unknown date"

        holder.time.text = "$startText - $endText"
        holder.date.text = dateText
    }

    override fun getItemCount(): Int = bookings.size

    fun submitList(list: List<Booking>) {
        bookings = list
        notifyDataSetChanged()
    }
}