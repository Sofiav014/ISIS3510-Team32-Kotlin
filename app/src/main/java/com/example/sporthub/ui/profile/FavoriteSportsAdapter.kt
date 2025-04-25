package com.example.sporthub.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.data.model.Sport

class FavoriteSportsAdapter(
    private var sports: List<Sport>
) : RecyclerView.Adapter<FavoriteSportsAdapter.SportViewHolder>() {

    fun updateSports(newSports: List<Sport>) {
        this.sports = newSports
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_sport, parent, false)
        return SportViewHolder(view)
    }

    override fun onBindViewHolder(holder: SportViewHolder, position: Int) {
        holder.bind(sports[position])
    }

    override fun getItemCount() = sports.size

    inner class SportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sportIcon: ImageView = itemView.findViewById(R.id.sportIcon)
        private val sportName: TextView = itemView.findViewById(R.id.sportName)

        fun bind(sport: Sport) {
            sportName.text = sport.name

            // Load sport logo using Glide
            if (sport.logo.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(sport.logo)
                    .into(sportIcon)
            } else {
                // Set a default image based on sport name
                when (sport.name.lowercase()) {
                    "basketball" -> sportIcon.setImageResource(R.drawable.ic_basketball_logo)
                    "football" -> sportIcon.setImageResource(R.drawable.ic_football_logo)
                    "volleyball" -> sportIcon.setImageResource(R.drawable.ic_volleyball_logo)
                    "tennis" -> sportIcon.setImageResource(R.drawable.ic_tennis_logo)
                    else -> sportIcon.setImageResource(R.drawable.ic_basketball_logo)
                }
            }
        }
    }
}