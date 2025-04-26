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
import com.example.sporthub.data.model.Venue
import com.example.sporthub.ui.findVenues.FindVenuesViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sporthub.databinding.FragmentVenueDetailBinding
import com.example.sporthub.ui.venueDetail.BookingAdapter
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.fragment.findNavController

class VenueDetailFragment : Fragment() {

    private val args: VenueDetailFragmentArgs by navArgs()
    private val viewModel: VenueDetailViewModel by viewModels()
    private lateinit var bookingsRecyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter

    private lateinit var venueImage: ImageView
    private lateinit var venueName: TextView
    private lateinit var venueLocation: TextView
    private lateinit var venueSport: TextView
    private lateinit var venueRating: TextView

    private val findVenuesViewModel: FindVenuesViewModel by activityViewModels()

    private var _binding: FragmentVenueDetailBinding? = null
    private val binding get() = _binding!!


    private var _bookingAdapter: BookingAdapter? = null
    private val bookingAdapter2 get() = _bookingAdapter!!

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

        venueImage = view.findViewById(R.id.venueImageDetail)
        venueName = view.findViewById(R.id.venueNameDetail)
        venueLocation = view.findViewById(R.id.venueLocationDetail)
        venueSport = view.findViewById(R.id.venueSportDetail)
        venueRating = view.findViewById(R.id.venueRatingDetail)

        bookingsRecyclerView = view.findViewById(R.id.recyclerViewBookings)

        bookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
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
                // Initialize adapter with venue name
                bookingAdapter = BookingAdapter(venue.name)

                // Set adapter and layout manager here (moved from earlier)
                bookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                bookingsRecyclerView.adapter = bookingAdapter

                // Set venue details
                venueName.text = venue.name
                venueLocation.text = venue.name
                venueSport.text = venue.sport?.name ?: "Sport name not available"
                venueRating.text = String.format("%.1f", venue.rating)

                // Load venue image
                Glide.with(requireContext())
                    .load(venue.image)
                    .into(venueImage)

                // Log and display bookings
                Log.d("DEBUG", "Fetched bookings: ${venue.bookings}")
                bookingAdapter.submitList(venue.bookings ?: emptyList())
            }
            else {
                venueName.text = "Venue not available"
                venueLocation.text = ""
                venueSport.text = ""
                venueRating.text = ""
                venueImage.setImageResource(R.drawable.ic_court_logo) // Optional placeholder image

                bookingAdapter = BookingAdapter("")
                bookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                bookingsRecyclerView.adapter = bookingAdapter
                bookingAdapter.submitList(emptyList())

                Snackbar.make(requireView(), "Unable to load venue details. Please check your connection.", Snackbar.LENGTH_LONG).show()
            }
        }

    }
    private fun setupRecyclerView() {
        _bookingAdapter = BookingAdapter(args.venue.name)
        binding.recyclerViewBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter2
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

                bookingAdapter2.submitList(it.bookings ?: emptyList())

            }
        }
    }

    private fun setupButton() {
        binding.btnCreateBooking.setOnClickListener {
            viewModel.venue.value?.let { venue ->

                android.util.Log.d("VenueDetailFragment", "Navigating with venue: ${venue.name} (${venue.id})")

                val action = VenueDetailFragmentDirections
                    .actionVenueDetailFragmentToNavigationCreate(venue)
                findNavController().navigate(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _bookingAdapter = null
        _binding = null // liberar binding para evitar memory leaks
    }
}
