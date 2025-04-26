package com.example.sporthub.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.example.sporthub.data.model.Venue
import com.example.sporthub.data.model.Sport
import com.example.sporthub.ui.home.viewholder.PopularityViewHolder


sealed class PopularityItem {
    data class VenueItem(
        val venue: Venue,
        val title: String,
        val additionalInfo: String? = null
    ) : PopularityItem()

    data class SportItem(
        val sport: Sport,
        val title: String,
        val additionalInfo: String? = null
    ) : PopularityItem()
}

class PopularityAdapter : ListAdapter<PopularityItem, PopularityViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularityViewHolder {
        return PopularityViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: PopularityViewHolder, position: Int) {
        val item = getItem(position)
        if (item is PopularityItem.SportItem && item.sport.id == "unknown") {
            holder.itemView.visibility = View.GONE
        } else {
            holder.itemView.visibility = View.VISIBLE
            holder.bind(item)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PopularityItem>() {
            override fun areItemsTheSame(oldItem: PopularityItem, newItem: PopularityItem): Boolean {
                return when {
                    oldItem is PopularityItem.VenueItem && newItem is PopularityItem.VenueItem ->
                        oldItem.venue.id == newItem.venue.id // Asegúrate de que Venue tiene `id`
                    oldItem is PopularityItem.SportItem && newItem is PopularityItem.SportItem ->
                        oldItem.sport.id == newItem.sport.id // Asegúrate de que Sport tiene `id`
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: PopularityItem, newItem: PopularityItem): Boolean {
                return oldItem == newItem
            }
        }
    }

}
