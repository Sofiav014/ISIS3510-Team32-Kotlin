package com.example.sporthub.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sporthub.data.repository.HomeRepository
import com.example.sporthub.databinding.FragmentHomeBinding
import com.example.sporthub.viewmodel.HomeViewModel
import com.example.sporthub.viewmodel.SharedUserViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: SharedUserViewModel by activityViewModels()
    private val homeViewModel by lazy { HomeViewModel(HomeRepository()) }

    private lateinit var popularityAdapter: PopularityAdapter
    private lateinit var upcomingBookingsAdapter: UpcomingBookingsAdapter
    private lateinit var recommendedBookingsAdapter: RecommendedBookingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupRecyclerViews()
        observeViewModels()

        return binding.root
    }

    private fun setupRecyclerViews() {
        popularityAdapter = PopularityAdapter()
        binding.recyclerPopularity.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = popularityAdapter
        }

        upcomingBookingsAdapter = UpcomingBookingsAdapter()
        binding.recyclerUpcomingBookings.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = upcomingBookingsAdapter
        }

        recommendedBookingsAdapter = RecommendedBookingsAdapter()
        binding.recyclerRecommendedBookings.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recommendedBookingsAdapter
        }
    }

    private fun observeViewModels() {
        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                homeViewModel.loadData(user)
            }
        }

        // Replace this in your HomeFragment.kt file to ensure the data is passed correctly

        homeViewModel.popularityReport.observe(viewLifecycleOwner) { report ->
            Log.d("PopularityReport", "Received report: $report")

            val items = listOfNotNull(
                // For the Best Rated venue, include the rating as additional info
                report.highestRatedVenue?.let {
                    PopularityItem.VenueItem(
                        it,
                        "Best Rated Overall",
                        "★ ${String.format("%.1f", it.rating)}"
                    )
                },
                // Include most played sport if it's not "unknown"
                report.mostPlayedSport.takeIf { it.id != "unknown" }?.let {
                    PopularityItem.SportItem(
                        it,
                        "Most Played by You",
                        "Played ${report.mostPlayedSportCount} times"
                    )
                },
                // For the Most Booked venue, include the booking count as additional info
                report.mostBookedVenue?.let {
                    PopularityItem.VenueItem(
                        it,
                        "Most Booked Overall",
                        "${report.mostBookedCount} bookings"
                    )
                }
            )

            Log.d("PopularityReport", "Items being submitted: $items")
            popularityAdapter.submitList(items)
        }

        homeViewModel.upcomingBookings.observe(viewLifecycleOwner) { bookings ->
            upcomingBookingsAdapter.submitList(bookings)

            Log.d("UpcomingBookings", "Upcoming bookings list: $bookings")

            // Show message if there are no bookings
            binding.textNoUpcomingBookings.visibility = if (bookings.isEmpty()) View.VISIBLE else View.GONE
        }

        homeViewModel.recommendedBookings.observe(viewLifecycleOwner) { bookings ->
            recommendedBookingsAdapter.submitList(bookings)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}