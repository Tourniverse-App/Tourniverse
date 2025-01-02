package com.example.tourniverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.TeamStanding

class TableStatisticsAdapter(
    private val teamStandings: List<TeamStanding>
) : RecyclerView.Adapter<TableStatisticsAdapter.TableViewHolder>() {

    /**
     * ViewHolder class for table statistics.
     */
    class TableViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val teamNameTextView: TextView = view.findViewById(R.id.teamNameTextView)
        val winsTextView: TextView = view.findViewById(R.id.winsTextView)
        val drawsTextView: TextView = view.findViewById(R.id.drawsTextView)
        val lossesTextView: TextView = view.findViewById(R.id.lossesTextView)
        val goalsTextView: TextView = view.findViewById(R.id.goalsTextView)
        val pointsTextView: TextView = view.findViewById(R.id.pointsTextView)
    }

    /**
     * Inflates the item layout for each row.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_statistics_table, parent, false)
        return TableViewHolder(view)
    }

    /**
     * Binds data to the ViewHolder.
     */
    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        val team = teamStandings[position]

        holder.teamNameTextView.text = team.teamName
        holder.winsTextView.text = team.wins.toString()
        holder.drawsTextView.text = team.draws.toString()
        holder.lossesTextView.text = team.losses.toString()
        holder.goalsTextView.text = team.goals.toString()
        holder.pointsTextView.text = team.points.toString()
    }

    /**
     * Returns the total number of items.
     */
    override fun getItemCount(): Int {
        return teamStandings.size
    }
}
