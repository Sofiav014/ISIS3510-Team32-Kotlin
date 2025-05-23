package com.example.sporthub.ui.venueDetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.databinding.FragmentVenueDetailBinding
import com.example.sporthub.viewmodel.VenueDetailViewModel

class VenueDetailFragment : Fragment() {

    private val args: VenueDetailFragmentArgs by navArgs()
    private val viewModel: VenueDetailViewModel by viewModels()

    private var _binding: FragmentVenueDetailBinding? = null
    private val binding get() = _binding!!

    // The adapter is no longer initialized immediately.
    private var bookingAdapter: BookingAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVenueDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setupRecyclerView will now ONLY set the layout manager
        setupRecyclerView()
        setupButtonListeners()
        observeViewModel()

        viewModel.fetchVenueById(args.venue.id)
    }

    private fun setupRecyclerView() {
        // We only prepare the RecyclerView here. The adapter will be set later.
        binding.recyclerViewBookings.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupButtonListeners() {
        binding.btnCreateBooking.setOnClickListener {
            viewModel.venue.value?.let { venue ->
                val action = VenueDetailFragmentDirections.actionVenueDetailFragmentToNavigationCreate(venue)
                findNavController().navigate(action)
            }
        }

        binding.btnFavorite.setOnClickListener {
            viewModel.toggleFavoriteStatus()
        }
    }

    private fun observeViewModel() {
        viewModel.venue.observe(viewLifecycleOwner) { venue ->
            if (venue != null) {
                // Update the main venue card UI
                binding.venueNameDetail.text = venue.name
                binding.venueLocationDetail.text = venue.locationName
                binding.venueSportDetail.text = venue.sport?.name ?: "Sport not available"
                binding.venueRatingDetail.text = String.format("%.1f", venue.rating)

                Glide.with(requireContext())
                    .load(venue.image)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(binding.venueImageDetail)


                // Now that we have the venue name, we create and set the adapter.
                bookingAdapter = BookingAdapter(venue.name)
                binding.recyclerViewBookings.adapter = bookingAdapter
                bookingAdapter?.submitList(venue.bookings ?: emptyList())


            } else {
                // Handle the case where venue data is null (e.g., error)
                // Clear the main card details and remove the adapter
                binding.venueNameDetail.text = "Venue not available"
                binding.recyclerViewBookings.adapter = null
            }
        }

        viewModel.isFavorite.observe(viewLifecycleOwner) { isFavorite ->
            if (isFavorite) {
                binding.btnFavorite.setImageResource(R.drawable.ic_heart_filled)
            } else {
                binding.btnFavorite.setImageResource(R.drawable.ic_heart_outline)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
