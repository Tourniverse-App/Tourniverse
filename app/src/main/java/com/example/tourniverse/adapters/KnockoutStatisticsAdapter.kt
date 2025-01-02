package com.example.tourniverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.Match

class KnockoutStatisticsAdapter(
    private val matches: List<Match>
) : RecyclerView.Adapter<KnockoutStatisticsAdapter.KnockoutViewHolder>() {

    /**
     * ViewHolder for knockout match item.
     */
    class KnockoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val matchTextView: TextView = view.findViewById(R.id.matchTextView)
    }

    /**
     * Creates new ViewHolder for each item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KnockoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_statistics_knockout, parent, false)
        return KnockoutViewHolder(view)
    }

    /**
     * Binds data to the ViewHolder.
     */
    override fun onBindViewHolder(holder: KnockoutViewHolder, position: Int) {
        val match = matches[position]
        holder.matchTextView.text = "${match.teamA} vs ${match.teamB}\n${match.scoreA} : ${match.scoreB}"
    }

    /**
     * Returns the total number of items.
     */
    override fun getItemCount(): Int {
        return matches.size
    }
}
