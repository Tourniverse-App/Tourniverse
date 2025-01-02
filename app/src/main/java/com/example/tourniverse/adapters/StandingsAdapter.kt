package com.example.tourniverse.adapters

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.Match

class StandingsAdapter(
    private val items: List<Match>,
    private val updateMatchScores: (Match, Int, Int) -> Unit
) : RecyclerView.Adapter<StandingsAdapter.StandingsViewHolder>() {

    companion object {
        private const val TAG = "StandingsAdapter"
    }

    class StandingsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val teamAName: TextView = view.findViewById(R.id.teamAName)
        val teamBName: TextView = view.findViewById(R.id.teamBName)
        val scoreA: EditText = view.findViewById(R.id.scoreA)
        val scoreB: EditText = view.findViewById(R.id.scoreB)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StandingsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_standing, parent, false)
        Log.d(TAG, "ViewHolder created")
        return StandingsViewHolder(view)
    }

    override fun onBindViewHolder(holder: StandingsViewHolder, position: Int) {
        val match = items[position]
        Log.d(TAG, "Binding ViewHolder for match: ${match.teamA} vs ${match.teamB}")

        // Populate data
        holder.teamAName.text = match.teamA
        holder.teamBName.text = match.teamB

        // Temporary flag to disable TextWatcher while updating text programmatically
        var isUpdating = false

        holder.scoreA.setText(match.scoreA.toString())
        holder.scoreB.setText(match.scoreB.toString())

        // Add TextWatcher to score fields
        holder.scoreA.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                try {
                    val newScoreA = s?.toString()?.toIntOrNull() ?: 0

                    // Highlight changes
                    if (newScoreA != match.scoreA) {
                        holder.scoreA.setTextColor(holder.itemView.context.getColor(R.color.black))
                    }

                    Log.d(TAG, "ScoreA changed: $newScoreA for match: ${match.teamA} vs ${match.teamB}")

                    // Ignore updates for 0-0 unless changed
                    if (newScoreA != 0 || match.scoreB != 0) {
                        isUpdating = true
                        updateMatchScores(match, newScoreA, match.scoreB)
                        isUpdating = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating ScoreA: ${e.message}")
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        holder.scoreB.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                try {
                    val newScoreB = s?.toString()?.toIntOrNull() ?: 0

                    // Highlight changes
                    if (newScoreB != match.scoreB) {
                        holder.scoreB.setTextColor(holder.itemView.context.getColor(R.color.black))
                    }

                    Log.d(TAG, "ScoreB changed: $newScoreB for match: ${match.teamA} vs ${match.teamB}")

                    // Ignore updates for 0-0 unless changed
                    if (match.scoreA != 0 || newScoreB != 0) {
                        isUpdating = true
                        updateMatchScores(match, match.scoreA, newScoreB)
                        isUpdating = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating ScoreB: ${e.message}")
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "Total items: ${items.size}")
        return items.size
    }
}
