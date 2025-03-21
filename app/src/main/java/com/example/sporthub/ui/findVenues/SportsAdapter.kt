package com.example.sporthub.ui.findVenues

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sporthub.R
import com.example.sporthub.data.model.Sport

class SportsAdapter(
    private val sportsList: List<Sport>,
    private val onItemClick: (Sport) -> Unit
) : RecyclerView.Adapter<SportsAdapter.SportViewHolder>() {

    inner class SportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sportImage: ImageView = view.findViewById(R.id.sportImage)
        val sportName: TextView = view.findViewById(R.id.sportName)

        fun bind(sport: Sport) {
            sportName.text = sport.name
            Glide.with(itemView.context).load(sport.logo).into(sportImage)

            itemView.setOnClickListener {
                onItemClick(sport)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sport_card, parent, false)
        return SportViewHolder(view)
    }

    override fun onBindViewHolder(holder: SportViewHolder, position: Int) {
        val sport = sportsList[position]
        holder.bind(sport)
    }

    override fun getItemCount() = sportsList.size
}