package com.example.sporthub.ui.bookings // Your actual fragment package

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
import com.example.sporthub.viewmodel.BookingsViewModel // Your ViewModel import
import java.text.SimpleDateFormat
import java.util.*

class BookingsFragment : Fragment() {

    private var _binding: FragmentBookingsBinding? = null
    private val binding get() = _binding!!

    private val bookingsViewModel: BookingsViewModel by viewModels()
    private lateinit var bookingAdapter: BookingAdapter

    // Date formatters for the top display
    private val dayFormatter = SimpleDateFormat("dd", Locale.getDefault())
    private val dayOfWeekFormatter = SimpleDateFormat("EEE", Locale.getDefault())
    private val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupEventListeners()
    }

    private fun setupRecyclerView() {
        // Initialize the adapter (using the version for item_my_booking.xml)
        bookingAdapter = BookingAdapter { booking ->
            // Handle booking item click
            Toast.makeText(context, "Clicked on ${booking.venue?.name}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
    }

    private fun setupObservers() {
        // Observe the selected date and update the text views at the top
        bookingsViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            updateDateTextViews(date)
        }

        // Observe the list of bookings and submit it to the adapter
        bookingsViewModel.bookingsForSelectedDate.observe(viewLifecycleOwner) { bookings ->
            bookingAdapter.submitList(bookings)
            // Here you could add logic to show a "No bookings found" message if the list is empty
        }

        // Observe the loading state to show/hide a progress bar (optional)
        bookingsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // e.g., binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun updateDateTextViews(date: Date) {
        binding.textViewDay.text = dayFormatter.format(date)
        binding.textViewDayOfWeek.text = dayOfWeekFormatter.format(date)
        binding.textViewMonthYear.text = monthYearFormatter.format(date)
    }

    private fun setupEventListeners() {
        // Set a click listener on the calendar icon to show the date picker
        binding.buttonOpenCalendar.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        // Start the date picker with the currently selected date from the ViewModel
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
                // When a new date is picked, update it in the ViewModel
                bookingsViewModel.setSelectedDate(newSelectedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important for preventing memory leaks
    }
}