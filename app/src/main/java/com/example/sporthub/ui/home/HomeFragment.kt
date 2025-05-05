package com.example.sporthub.ui.home

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sporthub.data.repository.HomeRepository
import com.example.sporthub.databinding.FragmentHomeBinding
import com.example.sporthub.viewmodel.HomeViewModel
import com.example.sporthub.viewmodel.SharedUserViewModel
import com.example.sporthub.utils.LoadingTimeTracker


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: SharedUserViewModel by activityViewModels()
    private val homeViewModel by lazy { HomeViewModel(HomeRepository()) }

    private lateinit var popularityAdapter: PopularityAdapter
    private lateinit var upcomingBookingsAdapter: UpcomingBookingsAdapter
    private lateinit var recommendedBookingsAdapter: RecommendedBookingsAdapter

    private var networkReceiver: BroadcastReceiver? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        LoadingTimeTracker.start()

        setupRecyclerViews()
        observeViewModels()

        return binding.root
    }

    private fun setupRecyclerViews() {
        popularityAdapter = PopularityAdapter()
        binding.recyclerPopularity.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = popularityAdapter
            binding.recyclerPopularity.isNestedScrollingEnabled = false
        }

        upcomingBookingsAdapter = UpcomingBookingsAdapter()
        binding.recyclerUpcomingBookings.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = upcomingBookingsAdapter
            binding.recyclerUpcomingBookings.isNestedScrollingEnabled = false

        }

        recommendedBookingsAdapter = RecommendedBookingsAdapter()
        binding.recyclerRecommendedBookings.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recommendedBookingsAdapter
            binding.recyclerRecommendedBookings.isNestedScrollingEnabled = false
            binding.root.post {
                binding.recyclerRecommendedBookings.requestLayout()
            }
        }
    }

    private fun observeViewModels() {
        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                homeViewModel.loadHomeData(requireContext(), user)
            }
        }

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
                        "Played ${report.mostPlayedSportCount} time(s)"
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

            LoadingTimeTracker.stopAndRecord("home view", requireContext())
        }

        homeViewModel.upcomingBookings.observe(viewLifecycleOwner) { bookings ->
            upcomingBookingsAdapter.submitList(bookings)

            Log.d("UpcomingBookings", "Upcoming bookings list: $bookings")

            // Show message if there are no bookings
            binding.textNoUpcomingBookings.visibility = if (bookings.isEmpty()) View.VISIBLE else View.GONE
        }

        homeViewModel.recommendedBookings.observe(viewLifecycleOwner) { bookings ->
            // Si estamos offline, no mostramos bookings recomendados
            if (homeViewModel.isOffline.value == true) {
                recommendedBookingsAdapter.submitList(emptyList())
            } else {
                recommendedBookingsAdapter.submitList(bookings)
            }
        }

        homeViewModel.isOffline.observe(viewLifecycleOwner) { offline ->
            binding.textRecommendedOfflineWarning.visibility =
                if (offline) View.VISIBLE else View.GONE
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        networkReceiver = NetworkReceiver {
            val user = userViewModel.currentUser.value
            user?.let {
                homeViewModel.loadHomeData(requireContext(), it)
            }
        }
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(networkReceiver, filter)
        userViewModel.currentUser.value?.let { user ->
            homeViewModel.loadHomeData(requireContext(), user)
        }
    }


    override fun onStop() {
        super.onStop()
        networkReceiver?.let {
            requireContext().unregisterReceiver(it)
        }
    }

    override fun onResume() {
        super.onResume()
        userViewModel.currentUser.value?.let { user ->
            homeViewModel.loadHomeData(requireContext(), user)
        }
    }
}