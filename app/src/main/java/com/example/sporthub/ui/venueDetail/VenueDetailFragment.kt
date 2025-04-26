package com.example.sporthub.ui.venueDetail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.data.model.Venue
import com.example.sporthub.ui.findVenues.FindVenuesViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sporthub.ui.venueDetail.BookingAdapter

class VenueDetailFragment : Fragment() {

    private val args: VenueDetailFragmentArgs by navArgs()
    private val viewModel: VenueDetailViewModel by viewModels()
    private lateinit var bookingsRecyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter

    private lateinit var venueImage: ImageView
    private lateinit var venueName: TextView
    private lateinit var venueLocation: TextView
    private lateinit var venueSport: TextView
    private lateinit var venueRating: TextView

    private val findVenuesViewModel: FindVenuesViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_venue_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        venueImage = view.findViewById(R.id.venueImageDetail)
        venueName = view.findViewById(R.id.venueNameDetail)
        venueLocation = view.findViewById(R.id.venueLocationDetail)
        venueSport = view.findViewById(R.id.venueSportDetail)
        venueRating = view.findViewById(R.id.venueRatingDetail)

        bookingsRecyclerView = view.findViewById(R.id.recyclerViewBookings)

        bookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())


        viewModel.fetchVenueById(args.venueId)

        viewModel.venue.observe(viewLifecycleOwner) { venue ->
            if (venue != null) {
                // Initialize adapter with venue name
                bookingAdapter = BookingAdapter(venue.name)

                // Set adapter and layout manager here (moved from earlier)
                bookingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                bookingsRecyclerView.adapter = bookingAdapter

                // Set venue details
                venueName.text = venue.name
                venueLocation.text = venue.name
                venueSport.text = venue.sport?.name ?: "Sport name not available"
                venueRating.text = String.format("%.1f", venue.rating)

                // Load venue image
                Glide.with(requireContext())
                    .load(venue.image)
                    .into(venueImage)

                // Log and display bookings
                Log.d("DEBUG", "Fetched bookings: ${venue.bookings}")
                println("Fetched bookings: ${venue.bookings}")
                bookingAdapter.submitList(venue.bookings ?: emptyList())
            }
        }

    }
}
