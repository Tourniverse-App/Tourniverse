package com.example.tourniverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.TeamStanding
import com.example.tourniverse.models.Match

class StatisticsAdapter (
    private val items: List<Any>, // Can be TeamStanding for tables or Match for knockout
    private val isKnockout: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TABLE = 1
        private const val VIEW_TYPE_KNOCKOUT = 2
    }

    // ViewHolder for Table statistics
    class TableViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val teamNameTextView: TextView = view.findViewById(R.id.teamNameTextView)
        val winsTextView: TextView = view.findViewById(R.id.winsTextView)
        val drawsTextView: TextView = view.findViewById(R.id.drawsTextView)
        val lossesTextView: TextView = view.findViewById(R.id.lossesTextView)
        val goalsTextView: TextView = view.findViewById(R.id.goalsTextView)
        val pointsTextView: TextView = view.findViewById(R.id.pointsTextView)
    }

    // ViewHolder for Knockout matches
    class KnockoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val matchTextView: TextView = view.findViewById(R.id.matchTextView)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isKnockout) VIEW_TYPE_KNOCKOUT else VIEW_TYPE_TABLE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_TABLE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_statistics_table, parent, false)
            TableViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_statistics_knockout, parent, false)
            KnockoutViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TableViewHolder && items[position] is TeamStanding) {
            val teamStat = items[position] as TeamStanding
            holder.teamNameTextView.text = teamStat.teamName
            holder.winsTextView.text = teamStat.wins.toString()
            holder.drawsTextView.text = teamStat.draws.toString()
            holder.lossesTextView.text = teamStat.losses.toString()
            holder.goalsTextView.text = teamStat.goals.toString()
            holder.pointsTextView.text = teamStat.points.toString()
        } else if (holder is KnockoutViewHolder && items[position] is Match) {
            val match = items[position] as Match
            holder.matchTextView.text = "${match.teamA} vs ${match.teamB} - Score: ${match.scoreA}:${match.scoreB}"
        }
    }

    override fun getItemCount(): Int = items.size
}
