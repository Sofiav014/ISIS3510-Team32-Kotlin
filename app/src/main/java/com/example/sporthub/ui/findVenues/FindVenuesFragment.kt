package com.example.sporthub.ui.findVenues

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sporthub.R
import com.example.sporthub.data.model.Sport
import com.example.sporthub.utils.ConnectivityHelper
import com.google.android.material.snackbar.Snackbar
import com.example.sporthub.viewmodel.FindVenuesViewModel

class FindVenuesFragment : Fragment() {

    private val viewModel: FindVenuesViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SportsAdapter
    private var connectionFlag = false


    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (ConnectivityHelper.isNetworkAvailable(context) && connectionFlag) {
                // Refresh the adapter to reload images
                recyclerView.adapter?.notifyDataSetChanged()

                Snackbar.make(requireView(), "Internet connection restored.", Snackbar.LENGTH_SHORT).show()
                connectionFlag = false

            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_find_venues, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewSports)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = SportsAdapter(viewModel.sportsList) { sport ->
            navigateToVenueList(sport)
        }
        recyclerView.adapter = adapter

        if (!ConnectivityHelper.isNetworkAvailable(requireContext())) {
            connectionFlag = true
            Snackbar.make(view, "You are offline. Some venues might not load.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Dismiss") {} // User can dismiss manually
                .show()
        }

    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(connectivityReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(connectivityReceiver)
    }

    private fun navigateToVenueList(sport: Sport) {
        val bundle = Bundle().apply {
            putString("sportId", sport.id)
            putString("sport", sport.name)
        }
        findNavController().navigate(R.id.action_findVenuesFragment_to_venueListFragment, bundle)
    }
}
