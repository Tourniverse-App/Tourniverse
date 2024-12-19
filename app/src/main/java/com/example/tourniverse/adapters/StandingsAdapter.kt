package com.example.tourniverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.TeamStanding
import com.example.tourniverse.models.Match

class StandingsAdapter(
    private val items: List<Any>
) : RecyclerView.Adapter<StandingsAdapter.StandingsViewHolder>() {

    class StandingsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textLine: TextView = view.findViewById(R.id.textLine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StandingsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_standing, parent, false)
        return StandingsViewHolder(view)
    }

    override fun onBindViewHolder(holder: StandingsViewHolder, position: Int) {
        val item = items[position]
        when (item) {
            is TeamStanding -> {
                holder.textLine.text = "${item.teamName} - Points: ${item.points} | Goals: ${item.goals} | Wins: ${item.wins} | Losses: ${item.losses}"
            }
            is Match -> {
                holder.textLine.text = "Match: ${item.teamA} vs ${item.teamB} - Score: ${item.scoreA} : ${item.scoreB}"
            }
            else -> {
                holder.textLine.text = "Unknown item type"
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
