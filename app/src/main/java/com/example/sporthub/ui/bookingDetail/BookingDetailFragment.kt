package com.example.sporthub.ui.bookingDetail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.databinding.FragmentBookingDetailBinding
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.fragment.findNavController
import com.example.sporthub.data.model.Booking
import com.example.sporthub.viewmodel.BookingDetailViewModel
import com.example.sporthub.viewmodel.CreateBookingViewModel
import com.example.sporthub.viewmodel.HomeViewModel
import com.example.sporthub.viewmodel.SharedUserViewModel
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat

class BookingDetailFragment : Fragment() {

    private var _binding: FragmentBookingDetailBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<BookingDetailFragmentArgs>()
    private val viewModel by viewModels<BookingDetailViewModel>()

    private val userViewModel     by activityViewModels<SharedUserViewModel>()
    private val createBookingVM   by activityViewModels<CreateBookingViewModel>()
    private val homeViewModel     by activityViewModels<HomeViewModel>()


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

        setupObservers()
        setupClickListeners()

        viewModel.loadBooking(args.bookingId)

        viewModel.booking.observe(viewLifecycleOwner) { booking ->
            Log.d("BookingDetail", "CurrentBooking: $booking")}

        // Observer for connectivity status
        viewModel.isOffline.observe(viewLifecycleOwner) { isOffline ->
            if (isOffline) {
                Toast.makeText(requireContext(),
                    "There is no internet connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.joinResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is BookingDetailViewModel.JoinResult.Success -> {

                    val booking = viewModel.booking.value!!
                    val user    = userViewModel.currentUser.value!!
                    lifecycleScope.launch {

                        createBookingVM.addBookingToUser(user.id, booking)


                        val updatedUser = user.copy(bookings = user.bookings + booking)
                        userViewModel.updateCurrentUser(updatedUser)
                        homeViewModel.loadHomeData(requireContext(), updatedUser)
                    }
                    Toast.makeText(requireContext(),
                        "You have successfully joined a booking",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is BookingDetailViewModel.JoinResult.Failure -> {
                    Toast.makeText(requireContext(),
                        "The request to join a booking was unsuccessful",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        viewModel.cancelResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is BookingDetailViewModel.CancelResult.Success -> {
                    val booking = viewModel.booking.value!!
                    val user = userViewModel.currentUser.value!!
                    lifecycleScope.launch {
                        // Remove booking from user
                        createBookingVM.removeBookingFromUser(user.id, booking)

                        // Update user and reload home data
                        val updatedUser = user.copy(bookings = user.bookings.filter { it.id != booking.id })
                        userViewModel.updateCurrentUser(updatedUser)
                        homeViewModel.loadHomeData(requireContext(), updatedUser)
                    }
                    Toast.makeText(requireContext(),
                        "Booking canceled successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is BookingDetailViewModel.CancelResult.Failure -> {
                    Toast.makeText(requireContext(),
                        "Failed to cancel booking: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }




    private fun setupObservers() {
        viewModel.booking.observe(viewLifecycleOwner) { booking ->
            binding.apply {
                // Header
                Glide.with(this@BookingDetailFragment)
                    .load(booking.venue?.image)
                    .placeholder(R.drawable.placeholder_image)
                    .into(headerImage)

                titleText.text = booking.venue?.name.orEmpty()
                locationHeaderText.text = booking.venue?.locationName.orEmpty()
                sportText.text = booking.venue?.sport?.name.orEmpty()

                val start = booking.startTime?.toDate()
                val end = booking.endTime?.toDate()

                val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

                val startText = start?.let { timeFormatter.format(it) } ?: "N/A"
                val endText = end?.let { timeFormatter.format(it) } ?: "N/A"
                val dateTxt = start?.let { dateFormatter.format(it) } ?: "Unknown date"

                dateText.text = dateTxt
                locationText.text = booking.venue?.locationName.orEmpty()
                timeText.text = "$startText - $endText"
                participantsText.text = "${booking.users.size} / ${booking.maxUsers}"

                // Status chip
                val now = Date()
                val isUpcoming  = start?.let { now.before(it) } ?: false
                val isFinished  = end  ?.let { now.after(it) }  ?: false

                statusChip.text = when {
                    isUpcoming -> "Upcoming"
                    isFinished -> "Finished"
                    start != null && end != null -> "In progress"
                    else -> "Unknown"
                }


                // Update join button state
                updateJoinButtonState(booking)
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            joinButton.setOnClickListener {
                // Check connectivity first and show immediate toast if offline
                viewModel.checkConnectivity(requireContext())
                if (viewModel.isOffline.value == true) {
                    Toast.makeText(requireContext(),
                        "There is no internet connection",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                val booking = viewModel.booking.value
                if (userId != null && booking != null) {
                    val isUserAlreadyJoined = booking.users.contains(userId)

                    if (isUserAlreadyJoined) {
                        // Cancel booking - pass context for connectivity check
                        Log.d("BookingDetail", "Attempting to cancel booking for user: $userId")
                        viewModel.cancelCurrentBooking(userId, requireContext())
                    } else {
                        // Join booking - pass context for connectivity check
                        Log.d("BookingDetail", "Attempting to join booking for user: $userId")
                        viewModel.joinCurrentBooking(userId, requireContext())
                    }
                }
            }
        }
    }

    private fun updateJoinButtonState(booking: Booking) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val isUserAlreadyJoined = currentUserId?.let { booking.users.contains(it) } ?: false
        val isBookingFull = booking.users.size >= booking.maxUsers
        val now = Date()
        val isBookingActive = booking.startTime?.toDate()?.let { now.before(it) } ?: false

        Log.d("BookingDetail", "UpdateJoinButtonState - UserId: $currentUserId, IsJoined: $isUserAlreadyJoined, IsFull: $isBookingFull, IsActive: $isBookingActive")

        binding.joinButton.apply {
            when {
                isUserAlreadyJoined -> {
                    text = "Cancel Booking"
                    isEnabled = isBookingActive // Only allow canceling if booking hasn't started
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red)
                }
                isBookingFull -> {
                    text = "Booking Full"
                    isEnabled = false
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green)
                }
                !isBookingActive -> {
                    text = "Booking Ended"
                    isEnabled = false
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green)
                }
                else -> {
                    text = "Join Booking"
                    isEnabled = true
                    backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.green)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}