package com.example.sporthub.ui.bookingDetail

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.data.model.Booking
import java.text.SimpleDateFormat
import java.util.*
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.sporthub.databinding.FragmentBookingDetailBinding


class BookingDetailAdapter(private val bookings: List<Booking>, private val onJoinClick: (Booking) -> Unit) :
    RecyclerView.Adapter<BookingDetailAdapter.BookingDetailViewHolder>() {
    class BookingDetailViewHolder(
        private val binding: FragmentBookingDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        private val timeFmt = SimpleDateFormat("HH:mm",   Locale.getDefault())

        fun bind(booking: Booking) {
            // HEADER
            binding.titleText.text = booking.venue?.name.orEmpty()
            binding.sportText.text = booking.venue?.sport?.name.orEmpty()
            //binding.ratingBar.rating = booking.venue?.rating ?: 0f
            Glide.with(binding.headerImage)
                .load(booking.venue?.image)
                .placeholder(R.drawable.placeholder_image)
                .into(binding.headerImage)

            // DETAILS
            val start = booking.startTime?.toDate() ?: Date()
            val end   = booking.endTime  ?.toDate() ?: Date()
            val durationMin = ((end.time - start.time) / 60000).toInt()

            binding.dateText.text         = dateFmt.format(start)
            binding.locationText.text     = booking.venue?.locationName.orEmpty()
            binding.timeText.text         = "${timeFmt.format(start)}  ($durationMin min)"
            binding.participantsText.text = "${booking.users.size} / ${booking.maxUsers}"

            // STATUS CHIP (Upcoming / In progress / Finished)
            val now = Date()
            val statusText = when {
                now.before(start) -> "Upcoming"
                now.after(end)   -> "Finished"
                else             -> "In progress"
            }
            binding.statusChip.text = statusText

            // JOIN BUTTON
            binding.joinButton.setOnClickListener {
                //onJoinClick(booking)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingDetailViewHolder {
        val binding = FragmentBookingDetailBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingDetailViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size
}