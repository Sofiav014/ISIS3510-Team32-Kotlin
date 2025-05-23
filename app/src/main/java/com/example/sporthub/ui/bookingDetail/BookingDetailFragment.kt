package com.example.sporthub.ui.bookingDetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.databinding.FragmentBookingDetailBinding
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class BookingDetailFragment : Fragment(){

    private var _binding: FragmentBookingDetailBinding? = null
    private val binding get() = _binding!!

    private val args      by navArgs<BookingDetailFragmentArgs>()
    private val viewModel by viewModels<BookingDetailViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Load the booking from Firestore
        viewModel.loadBooking(args.bookingId)

        // 2) Observe LiveData and bind to views
        viewModel.booking.observe(viewLifecycleOwner) { booking ->
            // Header
            Glide.with(this)
                .load(booking.venue?.image)
                .placeholder(R.drawable.placeholder_image)
                .into(binding.headerImage)

            binding.titleText.text = booking.venue?.name.orEmpty()
            binding.sportText.text = booking.venue?.sport?.name.orEmpty()

            // Details
            val start = booking.startTime?.toDate() ?: Date()
            val end   = booking.endTime?.toDate()   ?: Date()
            val minutes = ((end.time - start.time) / 60000).toInt()

            val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val timeFmt = SimpleDateFormat("HH:mm",    Locale.getDefault())

            binding.dateText.text         = dateFmt.format(start)
            binding.locationText.text     = booking.venue?.locationName.orEmpty()
            binding.timeText.text         = "${timeFmt.format(start)}  ($minutes min)"
            binding.participantsText.text = "${booking.users.size} / ${booking.maxUsers}"

            // Status chip
            val now = Date()
            binding.statusChip.text = when {
                now.before(start) -> "Upcoming"
                now.after(end)    -> "Finished"
                else              -> "In progress"
            }
        }

        // 3) Join button
        binding.joinButton.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser!!.uid
            viewModel.joinCurrentBooking(userId)
        }

        // optional: back navigation
        binding.backBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = BookingAdapter(emptyList()) { booking ->
            // On item click, navigate to detail, passing booking.id
            val action = BookingsFragmentDirections
                .actionBookingsFragmentToBookingDetailFragment(booking.id)
            findNavController().navigate(action)
        }

        binding.recyclerViewBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BookingsFragment.adapter
        }
    }

    private fun loadBookings() {
        // TODO: replace this with real Firestore fetch
        val mockVenue = Venue(
            id = "v1",
            name = "Cafam",
            locationName = "Club Cafam",
            rating = 4.5,
            sport = Sport(id = "s1", name = "Basketball", logo = ""),
            image = "https://via.placeholder.com/300"
        )

        val now = Calendar.getInstance()
        val booking = Booking(
            id = "b1",
            startTime = Timestamp(now.time),
            endTime = Timestamp(Date(now.time.time + 3600000)), // +1h
            maxUsers = 4,
            users = listOf("u1", "u2"),
            venue = mockVenue
        )

        // supply to adapter
        adapter = BookingAdapter(listOf(booking)) { booking ->
            val action = BookingsFragmentDirections
                .actionBookingsFragmentToBookingDetailFragment(booking.id)
            findNavController().navigate(action)
        }
        binding.recyclerViewBookings.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}