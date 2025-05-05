package com.example.sporthub.ui.venueDetail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.ui.findVenues.FindVenuesViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sporthub.databinding.FragmentVenueDetailBinding
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.fragment.findNavController

class VenueDetailFragment : Fragment() {

    private val args: VenueDetailFragmentArgs by navArgs()
    private val viewModel: VenueDetailViewModel by viewModels()

    private val findVenuesViewModel: FindVenuesViewModel by activityViewModels()

    private var _binding: FragmentVenueDetailBinding? = null
    private val binding get() = _binding!!

    private var _bookingAdapter: BookingAdapter? = null
    private val bookingAdapter get() = _bookingAdapter!!

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

        setupRecyclerView()
        setupVenueInfo()
        observeVenue()
        setupButton()

        val cachedVenue = findVenuesViewModel.venueCache.values
            .flatten()
            .firstOrNull { it.id == args.venue.id }

        if (cachedVenue != null) {
            viewModel.setVenueFromCache(cachedVenue)
        } else {
            viewModel.fetchVenueById(args.venue.id)
        }

        viewModel.venue.observe(viewLifecycleOwner) { venue ->
            if (venue != null) {
                binding.venueNameDetail.text = venue.name
                binding.venueLocationDetail.text = venue.locationName
                binding.venueSportDetail.text = venue.sport?.name ?: "Sport name not available"
                binding.venueRatingDetail.text = String.format("%.1f", venue.rating)

                Glide.with(binding.root.context)
                    .load(venue.image)
                    .into(binding.venueImageDetail)

                Log.d("DEBUG", "Fetched bookings: ${venue.bookings}")
                bookingAdapter.submitList(venue.bookings ?: emptyList())
            } else {
                binding.venueNameDetail.text = "Venue not available"
                binding.venueLocationDetail.text = ""
                binding.venueSportDetail.text = ""
                binding.venueRatingDetail.text = ""
                binding.venueImageDetail.setImageResource(R.drawable.ic_court_logo)

                bookingAdapter.submitList(emptyList())

                Snackbar.make(
                    requireView(),
                    "Unable to load venue details. Please check your connection.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupRecyclerView() {
        _bookingAdapter = BookingAdapter(args.venue.name)
        binding.recyclerViewBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
    }

    private fun setupVenueInfo() {
        viewModel.setVenue(args.venue)
    }

    private fun observeVenue() {
        viewModel.venue.observe(viewLifecycleOwner) { venue ->
            venue?.let {
                binding.venueNameDetail.text = it.name
                binding.venueLocationDetail.text = it.locationName
                binding.venueSportDetail.text = it.sport?.name ?: "Sport not available"
                binding.venueRatingDetail.text = String.format("%.1f", it.rating)

                Glide.with(binding.root.context)
                    .load(it.image)
                    .into(binding.venueImageDetail)
            }
        }
    }

    private fun setupButton() {
        binding.btnCreateBooking.setOnClickListener {
            viewModel.venue.value?.let { venue ->
                val action = VenueDetailFragmentDirections
                    .actionVenueDetailFragmentToNavigationCreate(venue)
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _bookingAdapter = null
        _binding = null
    }
}

