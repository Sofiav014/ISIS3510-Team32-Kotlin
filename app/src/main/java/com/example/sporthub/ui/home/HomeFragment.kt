package com.example.sporthub.ui.home

import android.content.BroadcastReceiver
import android.content.Context
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
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.repository.HomeRepository
import com.example.sporthub.databinding.FragmentHomeBinding
import com.example.sporthub.viewmodel.HomeViewModel
import com.example.sporthub.viewmodel.SharedUserViewModel
import com.example.sporthub.utils.LoadingTimeTracker
import com.google.android.material.snackbar.Snackbar
import com.example.sporthub.data.repository.UserRepository
import com.example.sporthub.viewmodel.CreateBookingViewModel


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: SharedUserViewModel by activityViewModels()
    private val homeViewModel by lazy { HomeViewModel(HomeRepository()) }
    private val createBookingViewModel: CreateBookingViewModel by activityViewModels()
    private val userRepository = UserRepository()

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

        upcomingBookingsAdapter = UpcomingBookingsAdapter(homeViewModel)
        binding.recyclerUpcomingBookings.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = upcomingBookingsAdapter
            binding.recyclerUpcomingBookings.isNestedScrollingEnabled = false
        }

        recommendedBookingsAdapter = RecommendedBookingsAdapter(homeViewModel) { booking ->
            joinBooking(booking)
        }

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
                report.highestRatedVenue?.let {
                    PopularityItem.VenueItem(it, "Best Rated Overall", "★ ${String.format("%.1f", it.rating)}")
                },
                report.mostPlayedSport.takeIf { it.id != "unknown" }?.let {
                    PopularityItem.SportItem(it, "Most Played by You", "Played ${report.mostPlayedSportCount} time(s)")
                },
                report.mostBookedVenue?.let {
                    PopularityItem.VenueItem(it, "Most Booked Overall", "${report.mostBookedCount} bookings")
                }
            )

            Log.d("PopularityReport", "Items being submitted: $items")
            popularityAdapter.submitList(items)

            LoadingTimeTracker.stopAndRecord("Home View", requireContext())
        }

        homeViewModel.upcomingBookings.observe(viewLifecycleOwner) { bookings ->
            upcomingBookingsAdapter.submitList(bookings)
            upcomingBookingsAdapter.notifyDataSetChanged()

            Log.d("UpcomingBookings", "Upcoming bookings list: $bookings")

            binding.textNoUpcomingBookings.visibility = if (bookings.isEmpty()) View.VISIBLE else View.GONE
        }

        homeViewModel.recommendedBookings.observe(viewLifecycleOwner) { bookings ->
            if (homeViewModel.isOffline.value == true) {
                recommendedBookingsAdapter.submitList(emptyList())
            } else {
                recommendedBookingsAdapter.submitList(bookings)
            }
        }

        homeViewModel.isOffline.observe(viewLifecycleOwner) { offline ->
            binding.textRecommendedOfflineWarning.visibility = if (offline) View.VISIBLE else View.GONE
        }

        createBookingViewModel.bookingCreatedEvent.observe(viewLifecycleOwner) {
            // Reload or refresh the list of bookings
            userViewModel.currentUser.value?.let { user ->
                homeViewModel.refreshBookings(user)
            }
        }

    }

    private fun joinBooking(booking: Booking) {
        userViewModel.currentUser.value?.let { user ->
            userRepository.joinBooking(user.id, booking).addOnSuccessListener {
                val updatedBookings = user.bookings.toMutableList().apply { add(booking) }
                val updatedUser = user.copy(bookings = updatedBookings)

                userViewModel.updateCurrentUser(updatedUser)  // Triggers UI update

                // Reload home data to show updated bookings
                homeViewModel.loadHomeData(requireContext(), updatedUser)

                Snackbar.make(binding.root, "You have joined the booking!", Snackbar.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Snackbar.make(binding.root, "Failed to join the booking: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshUpcomingBookings() {
        // Fetch the updated bookings and update the RecyclerView or UI component that displays bookings
        userViewModel.currentUser.value?.let { user ->
            homeViewModel.getUpcomingBookings(user)  // Pass the user parameter
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        networkReceiver = NetworkReceiver {
            userViewModel.currentUser.value?.let { user ->
                homeViewModel.loadHomeData(requireContext(), user)
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
        val isThemeChanging = requireContext()
            .getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_theme_changing", false)

        if (isThemeChanging) {
            Log.d("ThemeAware", "Skipping network operations during theme change")
            return
        }

        userViewModel.currentUser.value?.let { user ->
            homeViewModel.loadHomeData(requireContext(), user)
        }
    }
}
