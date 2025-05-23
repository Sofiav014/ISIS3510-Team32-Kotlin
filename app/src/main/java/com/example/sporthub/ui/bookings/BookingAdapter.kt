import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sporthub.data.model.Booking
import com.example.sporthub.databinding.ItemMyBookingBinding // <-- CHANGE HERE
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(private val onBookingClicked: (Booking) -> Unit) :
    ListAdapter<Booking, BookingAdapter.BookingViewHolder>(BookingDiffCallback()) {

    // ViewHolder now uses the new ItemMyBookingBinding
    class BookingViewHolder(private val binding: ItemMyBookingBinding) : // <-- CHANGE HERE
        RecyclerView.ViewHolder(binding.root) {

        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        // We no longer need the dateFormatter for the card

        fun bind(booking: Booking, onBookingClicked: (Booking) -> Unit) {
            binding.bookingTitle.text = booking.venue?.name ?: "Unknown Venue"
            binding.bookingLocation.text = booking.venue?.locationName ?: "Location not available"
            binding.bookingSpots.text = "${booking.users.size} / ${booking.maxUsers}"

            val startTimeString = booking.startTime?.toDate()?.let { timeFormatter.format(it) } ?: "N/A"
            val endTimeString = booking.endTime?.toDate()?.let { timeFormatter.format(it) } ?: "N/A"
            binding.bookingTime.text = "$startTimeString - $endTimeString"

            // The line that sets bookingDate.text is now gone!

            binding.root.setOnClickListener {
                onBookingClicked(booking)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemMyBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false) // <-- CHANGE HERE
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position), onBookingClicked)
    }
}

// DiffUtil remains the same
class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
    override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean {
        return oldItem.id == newItem.id
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean {
        return oldItem == newItem
    }
}