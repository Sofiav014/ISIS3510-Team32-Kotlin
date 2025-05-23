package com.example.sporthub.ui.bookings

import BookingAdapter
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sporthub.databinding.FragmentBookingsBinding
import com.example.sporthub.viewmodel.BookingsViewModel
import com.google.android.material.snackbar.Snackbar // Import Snackbar
import java.text.SimpleDateFormat
import java.util.*

class BookingsFragment : Fragment() {

    private var _binding: FragmentBookingsBinding? = null
    private val binding get() = _binding!!

    private val bookingsViewModel: BookingsViewModel by viewModels()
    private lateinit var bookingAdapter: BookingAdapter

    private var noConnectionSnackbar: Snackbar? = null


    private val dayFormatter = SimpleDateFormat("dd", Locale.getDefault())
    private val dayOfWeekFormatter = SimpleDateFormat("EEE", Locale.getDefault())
    private val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupEventListeners()
    }

    private fun setupObservers() {
        bookingsViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            updateDateTextViews(date)
        }

        bookingsViewModel.bookingsForSelectedDate.observe(viewLifecycleOwner) { bookings ->
            bookingAdapter.submitList(bookings)

            if (bookings.isEmpty()) {
                // Only show empty state if network IS available
                if (bookingsViewModel.isNetworkAvailable.value == true) {
                    binding.recyclerViewBookings.visibility = View.GONE
                    binding.textViewEmptyState.visibility = View.VISIBLE
                }
            } else {
                binding.recyclerViewBookings.visibility = View.VISIBLE
                binding.textViewEmptyState.visibility = View.GONE
            }
        }

        bookingsViewModel.isNetworkAvailable.observe(viewLifecycleOwner) { isAvailable ->
            // Enable/disable button based on network status
            binding.buttonOpenCalendar.isEnabled = isAvailable

            if (isAvailable) {
                // If network is back, dismiss the snackbar if it's showing
                noConnectionSnackbar?.dismiss()
            } else {
                // If no network, hide the list/empty-state and show the snackbar
                binding.recyclerViewBookings.visibility = View.GONE
                binding.textViewEmptyState.visibility = View.GONE
                showNoConnectionSnackbar()
            }
        }

        bookingsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // You might want to enhance this to show a loading indicator
        }
    }


    private fun showNoConnectionSnackbar() {
        // Use an indefinite snackbar that stays until dismissed or connection returns
        noConnectionSnackbar = Snackbar.make(binding.root, "No internet connection", Snackbar.LENGTH_INDEFINITE)
            .setAction("RETRY") {
                bookingsViewModel.onRetry()
            }
        noConnectionSnackbar?.show()
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter { booking ->
            Toast.makeText(context, "Clicked on ${booking.venue?.name}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
    }

    private fun updateDateTextViews(date: Date) {
        binding.textViewDay.text = dayFormatter.format(date)
        binding.textViewDayOfWeek.text = dayOfWeekFormatter.format(date)
        binding.textViewMonthYear.text = monthYearFormatter.format(date)
    }

    private fun setupEventListeners() {
        binding.buttonOpenCalendar.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        bookingsViewModel.selectedDate.value?.let {
            calendar.time = it
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val newSelectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)
                }.time
                bookingsViewModel.setSelectedDate(newSelectedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }
}