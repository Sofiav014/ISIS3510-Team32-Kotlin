import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sporthub.data.model.Booking
import com.example.sporthub.databinding.ItemUpcomingBookingBinding
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(private val bookings: List<Booking>) :
    RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(private val binding: ItemUpcomingBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val start = booking.startTime?.toDate()
            val end = booking.endTime?.toDate()

            binding.textVenueName.text = booking.venue?.name ?: "Unknown Venue"
            binding.textLocation.text = booking.venue?.locationName ?: "Location not available"
            binding.textSport.text = booking.venue?.sport?.name ?: "Sport not specified"
            binding.textRating.text = booking.venue?.rating?.toString() ?: "N/A"

            binding.root.contentDescription = "${formatter.format(start)} - ${formatter.format(end)}"

            // Cargar imagen si tienes una URL en venue (requiere Glide o similar)
            Glide.with(binding.root.context)
                .load(booking.venue?.image)
                .into(binding.imageBackground)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemUpcomingBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size
}