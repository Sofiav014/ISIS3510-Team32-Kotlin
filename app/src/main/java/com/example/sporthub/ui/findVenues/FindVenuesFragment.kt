package com.example.sporthub.ui.findVenues

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sporthub.R
import com.example.sporthub.data.model.Sport
import com.example.sporthub.ui.findVenues.SportsAdapter

class FindVenuesFragment : Fragment() {

    private val viewModel: FindVenuesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_find_venues, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewSports)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val adapter = SportsAdapter(viewModel.sportsList) { sport ->
            navigateToVenueList(sport)
        }

        recyclerView.adapter = adapter
    }

    private fun navigateToVenueList(sport: Sport) {
        val bundle = Bundle().apply {
            putString("sportId", sport.id)
        }
        findNavController().navigate(R.id.action_findVenuesFragment_to_venueListFragment, bundle)



    }
}