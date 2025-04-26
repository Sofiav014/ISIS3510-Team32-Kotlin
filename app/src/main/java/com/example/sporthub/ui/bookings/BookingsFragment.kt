package com.example.sporthub.ui.bookings

import BookingAdapter
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sporthub.data.model.Booking
import com.example.sporthub.data.model.Venue
import com.example.sporthub.databinding.FragmentBookingsBinding
import com.example.sporthub.data.model.Sport
import com.google.firebase.Timestamp
import java.util.Date

class BookingsFragment : Fragment() {

    private lateinit var binding: FragmentBookingsBinding
    private lateinit var adapter: BookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookingsBinding.inflate(inflater, container, false)

        setupRecyclerView()

        // Cargar datos simulados por ahora (reemplaza con datos reales si quieres)
        loadBookings()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = BookingAdapter(emptyList()) // se actualizará con data real
        //binding.recyclerViewBookings.layoutManager = LinearLayoutManager(requireContext())
        //binding.recyclerViewBookings.adapter = adapter
    }

    private fun loadBookings() {
        // Simulación de reservas (debes reemplazar con datos de Firebase)
        val mockVenue = Venue(
            id = "v1",
            name = "Cafam",
            locationName = "Club Cafam",
            rating = 4.5,
            sport = Sport(id = "s1", name = "Basketball", logo = ""),
            image = "https://via.placeholder.com/300"
        )
        val now = Calendar.getInstance()

        val booking = Booking(
            id = "b1",
            startTime = Timestamp(now.time),
            endTime = Timestamp(Date(now.time.time + 3600000)), // +1 hora
            maxUsers = 4,
            users = listOf("u1", "u2"),
            venue = mockVenue
        )

        adapter = BookingAdapter(listOf(booking))
        //binding.recyclerViewBookings.adapter = adapter
    }
}