package com.example.sporthub.ui.home.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sporthub.R
import com.example.sporthub.databinding.ItemPopularityBinding
import com.example.sporthub.ui.home.PopularityItem

class PopularityViewHolder(private val binding: ItemPopularityBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: PopularityItem) {
        binding.textTitle.text = when (item) {
            is PopularityItem.VenueItem -> item.title
            is PopularityItem.SportItem -> item.title
        }

        binding.textDescription.text = when (item) {
            is PopularityItem.VenueItem -> item.venue.name
            is PopularityItem.SportItem -> item.sport.name
        }

        binding.iconImage.setImageResource(when (item) {
            is PopularityItem.VenueItem -> R.drawable.location_star

            is PopularityItem.SportItem -> {
                when (item.sport.name.lowercase()) {
                    "basketball" -> R.drawable.ic_basketball_logo
                    "football" -> R.drawable.ic_football_logo
                    "tennis" -> R.drawable.ic_tennis_logo
                    "volleyball" -> R.drawable.ic_volleyball_logo
                    else -> R.drawable.ic_basketball_logo
                }
            }

        })
    }

    companion object {
        fun create(parent: ViewGroup): PopularityViewHolder {
            val binding = ItemPopularityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PopularityViewHolder(binding)
        }
    }
}
