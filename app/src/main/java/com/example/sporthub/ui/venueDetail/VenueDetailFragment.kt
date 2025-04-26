package com.example.sporthub.ui.venueDetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.sporthub.databinding.FragmentVenueDetailBinding

class VenueDetailFragment : Fragment() {

    private var _binding: FragmentVenueDetailBinding? = null
    private val binding get() = _binding!!

    private val args: VenueDetailFragmentArgs by navArgs()
    private val viewModel: VenueDetailViewModel by viewModels()

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

                bookingAdapter.submitList(it.bookings ?: emptyList())

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
