package com.example.tourniverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.TeamStatistics

class StatisticsAdapter(private val teamStatistics: List<TeamStatistics>) : RecyclerView.Adapter<StatisticsAdapter.StatisticsViewHolder>() {

    inner class StatisticsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val teamNameTextView: TextView = view.findViewById(R.id.teamNameTextView)
        val winsTextView: TextView = view.findViewById(R.id.winsTextView)
        val lossesTextView: TextView = view.findViewById(R.id.lossesTextView)
        val fieldGoalsTextView: TextView = view.findViewById(R.id.fieldGoalsTextView)
        val pointsTextView: TextView = view.findViewById(R.id.pointsTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_statistics, parent, false)
        return StatisticsViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatisticsViewHolder, position: Int) {
        val stats = teamStatistics[position]
        holder.teamNameTextView.text = stats.teamName
        holder.winsTextView.text = stats.wins.toString()
        holder.lossesTextView.text = stats.losses.toString()
        holder.fieldGoalsTextView.text = stats.fieldGoals.toString()
        holder.pointsTextView.text = stats.points.toString()
    }

    override fun getItemCount(): Int = teamStatistics.size
}
