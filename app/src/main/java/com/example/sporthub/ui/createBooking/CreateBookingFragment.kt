package com.example.sporthub.ui.createBooking

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.sporthub.R
import com.example.sporthub.data.model.Venue
import com.example.sporthub.databinding.FragmentCreateBookingBinding
import com.example.sporthub.viewmodel.CreateBookingViewModel
import com.example.sporthub.viewmodel.SharedUserViewModel
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class CreateBookingFragment : Fragment() {

    private val viewModel: CreateBookingViewModel by viewModels()
    private lateinit var binding: FragmentCreateBookingBinding
    private val userViewModel: SharedUserViewModel by activityViewModels()

    private val timeSlots = listOf(
        "07:00 - 08:00", "08:00 - 09:00", "09:00 - 10:00", "10:00 - 11:00", "11:00 - 12:00",
        "12:00 - 13:00", "13:00 - 14:00", "14:00 - 15:00", "15:00 - 16:00",
        "16:00 - 17:00", "17:00 - 18:00", "18:00 - 19:00"
    )
    private var selectedTimeSlot: String? = null
    private var selectedDate: LocalDate? = null
    private val dateStringFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val args: CreateBookingFragmentArgs by navArgs()
    private lateinit var venue1: Venue

    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeSlotsRunnable = object : Runnable {
        override fun run() {
            selectedDate = LocalDate.now()
            setupTimeSlotButtons()
            handler.postDelayed(this, 60_000)
        }
    }

    // Adding NetworkCallback for real-time connectivity monitoring
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            activity?.runOnUiThread {
                viewModel.checkConnectivity(requireContext())
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            activity?.runOnUiThread {
                viewModel.checkConnectivity(requireContext())
            }
        }
    }

    private var isOnline: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateBookingBinding.inflate(inflater, container, false)
        venue1 = args.venue

        selectedDate = LocalDate.now()
        setupDatePicker()
        setupTimeSlotButtons()
        setupPlayerCountSpinner()
        observeViewModel()

        binding.btnCreateReservation.setOnClickListener {
            // Force an immediate connectivity check
            viewModel.checkConnectivity(requireContext())

            Handler(Looper.getMainLooper()).postDelayed({
                if (viewModel.isOffline.value == true) {
                    Toast.makeText(requireContext(), "Cannot create a booking without internet connection", Toast.LENGTH_SHORT).show()
                } else {
                    createReservation()
                }
            }, 100)
        }

        handler.post(updateTimeSlotsRunnable)

        return binding.root
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        binding.calendarView.minDate = calendar.timeInMillis
        binding.calendarView.date = calendar.timeInMillis

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            setupTimeSlotButtons()

            handler.removeCallbacks(updateTimeSlotsRunnable)
            if (isToday(selectedDate)) {
                handler.post(updateTimeSlotsRunnable)
            }
        }
    }

    private fun isToday(date: LocalDate?): Boolean {
        return date != null && date == LocalDate.now()
    }

    private fun setupTimeSlotButtons() {
        val now = LocalTime.now()
        binding.timeSlotGrid.removeAllViews()

        timeSlots.forEach { slot ->
            val startHour = slot.split(" - ")[0]
            val slotTime = LocalTime.parse(startHour, DateTimeFormatter.ofPattern("HH:mm"))

            if (isToday(selectedDate) && slotTime.isBefore(now)) {
                return@forEach
            }

            val button = Button(requireContext()).apply {
                text = slot
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_time_slot_button)
                setTextColor(Color.parseColor("#60508C"))
                setPadding(0, 16, 0, 16)
                textSize = 12f
                isAllCaps = false
                stateListAnimator = null
                isSelected = slot == selectedTimeSlot
                setOnClickListener {
                    selectedTimeSlot = slot
                    updateTimeSlotSelection(this)
                }
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }

            button.layoutParams = params
            binding.timeSlotGrid.addView(button)
        }

        if (selectedTimeSlot != null && !timeSlotsAvailableNow().contains(selectedTimeSlot)) {
            selectedTimeSlot = null
        }
    }

    private fun timeSlotsAvailableNow(): List<String> {
        val now = LocalTime.now()
        return timeSlots.filter { slot ->
            val startHour = slot.split(" - ")[0]
            val slotTime = LocalTime.parse(startHour, DateTimeFormatter.ofPattern("HH:mm"))
            !(isToday(selectedDate) && slotTime.isBefore(now))
        }
    }

    private fun updateTimeSlotSelection(selectedButton: Button) {
        for (i in 0 until binding.timeSlotGrid.childCount) {
            val child = binding.timeSlotGrid.getChildAt(i)
            if (child is Button) {
                child.isSelected = child == selectedButton
            }
        }
    }

    private fun setupPlayerCountSpinner() {
        val players = (1..6).map { it.toString() }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, players)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.playerCountSpinner.adapter = adapter
    }

    private fun isDateInPast(): Boolean {
        return selectedDate?.isBefore(LocalDate.now()) ?: false
    }

    private fun createReservation() {
        val dateString = selectedDate?.format(dateStringFormatter)
        val selectedPlayers = binding.playerCountSpinner.selectedItem?.toString()
        val selectedTime = selectedTimeSlot
        val user = userViewModel.currentUser.value

        if (user == null) {
            Toast.makeText(context, "User not available", Toast.LENGTH_SHORT).show()
            return
        }

        when {
            selectedDate == null -> {
                Toast.makeText(context, "Please select a date.", Toast.LENGTH_SHORT).show()
                return
            }
            isDateInPast() -> {
                Toast.makeText(context, "Selected date is in the past", Toast.LENGTH_SHORT).show()
                return
            }
            selectedTime == null -> {
                Toast.makeText(context, "Please select a time slot.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        viewModel.createReservation(
            date = dateString ?: "",
            timeSlot = selectedTime!!,
            players = selectedPlayers?.toInt() ?: 1,
            userId = user.id,
            venue = venue1
        )
    }

    private fun observeViewModel() {
        viewModel.reservationResult.observe(viewLifecycleOwner) { success ->
            val msg = if (success) "Reservation created successfully" else "Failed to create reservation"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
        viewModel.bookingCreated.observe(viewLifecycleOwner) { created ->
            if (created) {
                findNavController().navigate(R.id.action_navigation_create_to_navigation_home)
            }
        }
        viewModel.isOffline.observe(viewLifecycleOwner) { offline ->
            isOnline = !offline

            if (offline) {
                binding.btnCreateReservation.alpha = 0.5f  // Dim the button
            } else {
                binding.btnCreateReservation.alpha = 1.0f  // Normal opacity
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.checkConnectivity(requireContext())
        handler.post(updateTimeSlotsRunnable)

        // Register network callback to track connectivity changes
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val request = NetworkRequest.Builder().build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
    }

    override fun onStop() {
        super.onStop()
        // Unregister network callback
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateTimeSlotsRunnable)
    }
}