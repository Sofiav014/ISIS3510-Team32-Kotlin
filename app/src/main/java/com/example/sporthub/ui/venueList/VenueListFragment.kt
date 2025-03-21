package com.example.sporthub.ui.venueList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sporthub.R
import com.example.sporthub.ui.findVenues.FindVenuesViewModel
import com.example.sporthub.ui.venueList.adapter.VenueAdapter

class VenueListFragment : Fragment() {

    private val viewModel: FindVenuesViewModel by viewModels()
    private lateinit var venueAdapter: VenueAdapter

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

        // Observe venue list and update adapter
        viewModel.venues.observe(viewLifecycleOwner, Observer { venues ->
            venueAdapter.submitList(venues)
        })

        // Fetch venues filtered by sportId
        viewModel.fetchVenuesBySport(sportId)
    }
}