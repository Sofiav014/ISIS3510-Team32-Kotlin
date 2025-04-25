package com.example.sporthub.ui.venueList

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sporthub.R
import com.example.sporthub.data.model.Venue
import com.example.sporthub.ui.findVenues.FindVenuesViewModel
import com.example.sporthub.ui.venueList.adapter.VenueAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import android.widget.TextView

class VenueListFragment : Fragment() {

    private val viewModel: FindVenuesViewModel by viewModels()
    private lateinit var venueAdapter: VenueAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null
    private var sportId: String? = null
    private var venuesLoaded = false
    private var cancellationTokenSource = CancellationTokenSource()
    private var sportName: String? = "Unknown"

    // Launcher para solicitar permisos de ubicación
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, obtener ubicación
            getCurrentLocation()
        } else {
            // Permiso denegado, cargar venues sin ordenar por distancia
            Toast.makeText(requireContext(), "Location permission denied. Venues will not be sorted by distance.", Toast.LENGTH_SHORT).show()
            loadVenues()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_venue_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sportId = arguments?.getString("sportId")
        if (sportId == null) {
            findNavController().navigateUp()
            return
        }

        sportName = arguments?.getString("sport") ?: "Unknown"

        val titleTextView = view.findViewById<TextView>(R.id.textViewVenueTitle)
        titleTextView.text = String.format(Locale.getDefault(), "%s Venues List", sportName)

        (requireActivity() as AppCompatActivity).findViewById<TextView>(R.id.toolbarTitle)?.text = "$sportName Venues List"

        setupRecyclerView(view)
        setupObservers()
        checkLocationPermission()
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewVenues)
        venueAdapter = VenueAdapter { selectedVenue ->
            val action = VenueListFragmentDirections
                .actionVenueListFragmentToVenueDetailFragment(selectedVenue.id)
            println("Selected Venue ID: ${selectedVenue.id}")
            findNavController().navigate(action)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = venueAdapter
        }
    }


    private fun setupObservers() {
        viewModel.venues.observe(viewLifecycleOwner, Observer { venues ->
            venuesLoaded = true
            val sortedVenues = if (userLocation != null) {
                sortVenuesByDistance(venues)
            } else {
                venues
            }
            venueAdapter.submitList(sortedVenues)

            // Pass user location to adapter to display distances
            venueAdapter.setUserLocation(userLocation)
        })
    }

    private fun checkLocationPermission() {
        when {
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Ya tenemos permiso, obtener ubicación
                getCurrentLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Explicar al usuario por qué necesitamos el permiso
                Toast.makeText(
                    requireContext(),
                    "Location permission is needed to show venues closest to you",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                // Solicitar permiso directamente
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {

        cancellationTokenSource = CancellationTokenSource()
        // Usamos getCurrentLocation en lugar de lastLocation para mayor precisión
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            userLocation = location

            // Pass the location to the adapter
            venueAdapter.setUserLocation(location)

            // Si ya teníamos los venues cargados, los reordenamos
            if (venuesLoaded) {
                val currentVenues = viewModel.venues.value
                if (currentVenues != null) {
                    val sortedVenues = sortVenuesByDistance(currentVenues)
                    venueAdapter.submitList(sortedVenues)
                }
            }

            // Cargar los venues (si no estaban cargados aún)
            if (!venuesLoaded) {
                loadVenues()
            }
        }.addOnFailureListener { e ->
            // Error al obtener ubicación
            Toast.makeText(
                requireContext(),
                "Failed to get location: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            loadVenues()
        }
    }

    private fun loadVenues() {
        sportId?.let { id ->
            viewModel.fetchVenuesBySport(id)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancellationTokenSource.cancel()
    }
}