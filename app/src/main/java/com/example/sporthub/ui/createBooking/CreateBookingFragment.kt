package com.example.sporthub.ui.createBooking

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.example.sporthub.databinding.FragmentCreateBookingBinding
import com.example.sporthub.viewmodel.CreateBookingViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.*
import java.time.format.DateTimeFormatter

class CreateBookingFragment : Fragment() {

    private val viewModel: CreateBookingViewModel by viewModels()
    private lateinit var binding: FragmentCreateBookingBinding

    private val timeSlots = listOf(
        "9:00 - 10:00", "10:00 - 11:00", "11:00 - 12:00",
        "14:00 - 15:00", "16:00 - 17:00", "17:00 - 18:00"
    )
    private var selectedTimeSlot: String? = null
    private var selectedDate: LocalDate? = null
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    private val dateStringFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val args: CreateBookingFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateBookingBinding.inflate(inflater, container, false)

        setupDatePicker()
        setupTimeSlotButtons()
        setupPlayerCountSpinner()
        observeViewModel()

        binding.btnCreateReservation.setOnClickListener {
            createReservation()
        }

        return binding.root
    }

    private fun setupDatePicker() {
        binding.selectedDateText.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select a date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val instant = Instant.ofEpochMilli(selection)
                val zoneDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                selectedDate = zoneDateTime
                binding.selectedDateText.text = zoneDateTime.format(dateFormatter)
            }

            datePicker.show(parentFragmentManager, "MATERIAL_DATE_PICKER")
        }
    }

    private fun setupTimeSlotButtons() {
        binding.timeSlotGrid.removeAllViews()
        timeSlots.forEach { slot ->
            val button = Button(requireContext()).apply {
                text = slot
                setOnClickListener {
                    selectedTimeSlot = slot
                    updateTimeSlotSelection(this)
                }
            }
            binding.timeSlotGrid.addView(button)
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
        val venueId = args.venueId

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
            timeSlot = selectedTime ?: "",
            players = selectedPlayers?.toInt() ?: 1,
            userId = "someUserId",
            venueId = venueId
        )
    }

    private fun observeViewModel() {
        viewModel.reservationResult.observe(viewLifecycleOwner) { success ->
            val msg = if (success) "Reservation created successfully" else "Failed to create reservation"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
}
