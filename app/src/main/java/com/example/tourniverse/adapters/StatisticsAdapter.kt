package com.example.tourniverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.TeamStanding

class StatisticsAdapter(
    private val teamStats: MutableList<TeamStanding>
) : RecyclerView.Adapter<StatisticsAdapter.StatisticsViewHolder>() {

    // ViewHolder to represent each row in the RecyclerView
    class StatisticsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val teamNameTextView: TextView = view.findViewById(R.id.teamNameTextView)
        val winsTextView: TextView = view.findViewById(R.id.winsTextView)
        val lossesTextView: TextView = view.findViewById(R.id.lossesTextView)
        val fieldGoalsTextView: TextView = view.findViewById(R.id.fieldGoalsTextView)
        val pointsTextView: TextView = view.findViewById(R.id.pointsTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_statistics, parent, false)
        return StatisticsViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatisticsViewHolder, position: Int) {
        val teamStat = teamStats[position]

        // Bind the data to the ViewHolder
        holder.teamNameTextView.text = teamStat.teamName
        holder.winsTextView.text = teamStat.wins.toString()
        holder.lossesTextView.text = teamStat.losses.toString()
        holder.fieldGoalsTextView.text = teamStat.goals.toString()
        holder.pointsTextView.text = teamStat.points.toString()
    }

    override fun getItemCount(): Int = teamStats.size

    // Optional: Update data in the adapter
    fun updateData(newStats: List<TeamStanding>) {
        teamStats.clear()
        teamStats.addAll(newStats)
        notifyDataSetChanged()
    }
}
