package com.example.tourniverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.Match

class MatchAdapter(private val matches: List<Match>) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    inner class MatchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val homeTeamTextView: TextView = view.findViewById(R.id.homeTeamTextView)
        val awayTeamTextView: TextView = view.findViewById(R.id.awayTeamTextView)
        val scoreTextView: TextView = view.findViewById(R.id.scoreTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matches[position]
        holder.homeTeamTextView.text = match.homeTeam
        holder.awayTeamTextView.text = match.awayTeam
        holder.scoreTextView.text = match.score
    }

    override fun getItemCount(): Int = matches.size
}
