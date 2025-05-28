package com.example.sporthub.ui.bookings

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
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class BookingsFragment : Fragment() {

    private var _binding: FragmentBookingsBinding? = null
    private val binding get() = _binding!!

    private val bookingsViewModel: BookingsViewModel by viewModels()
    private lateinit var bookingAdapter: BookingsAdapter

    private var noConnectionSnackbar: Snackbar? = null

    private val dayFormatter = SimpleDateFormat("dd", Locale.getDefault())
    private val dayOfWeekFormatter = SimpleDateFormat("EEE", Locale.getDefault())
    private val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    // This property will hold a reference to the inflated view from the stub
    private var emptyStateView: View? = null

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

    private fun setupObservers() {
        bookingsViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            updateDateTextViews(date)
        }

        bookingsViewModel.bookingsForSelectedDate.observe(viewLifecycleOwner) { bookings ->
            bookingAdapter.submitList(bookings)

            if (bookings.isEmpty()) {
                if (bookingsViewModel.isNetworkAvailable.value == true) {
                    binding.recyclerViewBookings.visibility = View.GONE

                    // If the empty state view hasn't been inflated yet, inflate it.
                    if (emptyStateView == null) {
                        emptyStateView = binding.viewStubEmptyState.inflate()
                    }
                    // Now that we know it's inflated, make it visible.
                    emptyStateView?.visibility = View.VISIBLE
                }
            } else {
                binding.recyclerViewBookings.visibility = View.VISIBLE
                // If the view has been inflated before, make sure it's hidden.
                emptyStateView?.visibility = View.GONE
            }
        }

        bookingsViewModel.isNetworkAvailable.observe(viewLifecycleOwner) { isAvailable ->
            binding.buttonOpenCalendar.isEnabled = isAvailable

            if (isAvailable) {
                noConnectionSnackbar?.dismiss()
            } else {
                binding.recyclerViewBookings.visibility = View.GONE
                // Also hide the stub-inflated view if there's no connection
                emptyStateView?.visibility = View.GONE
                showNoConnectionSnackbar()
            }
        }

        bookingsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
        }
    }

    private fun showNoConnectionSnackbar() {
        noConnectionSnackbar = Snackbar.make(binding.root, "No internet connection", Snackbar.LENGTH_INDEFINITE)
            .setAction("RETRY") {
                bookingsViewModel.onRetry()
            }
        noConnectionSnackbar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingsAdapter { booking ->
            Toast.makeText(context, "Clicked on ${booking.venue?.name}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
            setHasFixedSize(true)
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