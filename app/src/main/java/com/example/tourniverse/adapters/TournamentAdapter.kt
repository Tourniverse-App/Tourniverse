package com.example.tourniverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.Tournament

class TournamentAdapter(private val tournaments: List<Tournament>) :
    RecyclerView.Adapter<TournamentAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tournamentName: TextView = itemView.findViewById(R.id.tvTournamentName)
        val tournamentType: TextView = itemView.findViewById(R.id.tvTournamentType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tournament, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tournament = tournaments[position]
        holder.tournamentName.text = tournament.name
        holder.tournamentType.text = tournament.type
    }

    override fun getItemCount() = tournaments.size
}
