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

    private lateinit var bookingAdapter: BookingAdapter

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
        setupButtonListeners()
        observeViewModel()

        viewModel.fetchVenueById(args.venue.id)
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter("")
        binding.recyclerViewBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
    }

    private fun setupButtonListeners() {
        binding.btnCreateBooking.setOnClickListener {
            viewModel.venue.value?.let { venue ->
                val action = VenueDetailFragmentDirections.actionVenueDetailFragmentToNavigationCreate(venue)
                findNavController().navigate(action)
            }
        }

        binding.btnFavorite.setOnClickListener {
            // ViewModel handles all the logic
            viewModel.toggleFavoriteStatus()
        }
    }

    private fun observeViewModel() {
        viewModel.venue.observe(viewLifecycleOwner) { venue ->
            venue?.let {
                binding.venueNameDetail.text = it.name
                binding.venueLocationDetail.text = it.locationName
                binding.venueSportDetail.text = it.sport?.name ?: "Sport not available"
                binding.venueRatingDetail.text = String.format("%.1f", it.rating)

                Glide.with(requireContext())
                    .load(it.image)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(binding.venueImageDetail)

                bookingAdapter.submitList(it.bookings ?: emptyList())
            }
        }

        // Observer for the favorite status
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
