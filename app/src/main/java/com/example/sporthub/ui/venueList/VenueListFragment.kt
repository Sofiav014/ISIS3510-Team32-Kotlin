package com.example.sporthub.ui.venueList

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sporthub.R
import com.example.sporthub.data.model.Venue
import com.example.sporthub.ui.findVenues.FindVenuesViewModel
import com.example.sporthub.ui.venueList.adapter.VenueAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class VenueListFragment : Fragment() {

    private val viewModel: FindVenuesViewModel by viewModels()
    private lateinit var venueAdapter: VenueAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_venue_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sportId = arguments?.getString("sportId") ?: return

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewVenues)
        venueAdapter = VenueAdapter()

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = venueAdapter
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Get user location and then fetch venues
        getUserLocation {
            viewModel.venues.observe(viewLifecycleOwner, Observer { venues ->
                val sortedVenues = sortVenuesByDistance(venues)
                venueAdapter.submitList(sortedVenues)
            })
            viewModel.fetchVenuesBySport(sportId)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation(callback: () -> Unit) {
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            userLocation = location
            callback()
        }
    }

    private fun sortVenuesByDistance(venues: List<Venue>): List<Venue> {
        val currentLocation = userLocation ?: return venues

        return venues.sortedBy { venue ->
            val venueLocation = Location("").apply {
                latitude = venue.coords?.latitude ?: 0.0
                longitude = venue.coords?.longitude ?: 0.0
            }
            currentLocation.distanceTo(venueLocation)
        }
    }
}
