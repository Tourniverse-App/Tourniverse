package com.example.tourniverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.Match

class MatchAdapter(
    private val matches: List<Match>,
    private val onUpdateScore: (matchId: String, newScoreA: Int, newScoreB: Int) -> Unit
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matches[position]
        holder.bind(match, onUpdateScore)
    }

    override fun getItemCount(): Int = matches.size

    class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val teamATextView: TextView = itemView.findViewById(R.id.teamA)
        private val teamBTextView: TextView = itemView.findViewById(R.id.teamB)
        private val scoreAText: EditText = itemView.findViewById(R.id.scoreAInput)
        private val scoreBText: EditText = itemView.findViewById(R.id.scoreBInput)
        private val updateButton: Button = itemView.findViewById(R.id.updateScoreButton)

        fun bind(match: Match, onUpdateScore: (matchId: String, newScoreA: Int, newScoreB: Int) -> Unit) {
            teamATextView.text = match.teamA
            teamBTextView.text = match.teamB
            scoreAText.setText(match.scoreA.toString())
            scoreBText.setText(match.scoreB.toString())

            updateButton.setOnClickListener {
                val newScoreA = scoreAText.text.toString().toIntOrNull() ?: 0
                val newScoreB = scoreBText.text.toString().toIntOrNull() ?: 0
                onUpdateScore(match.id, newScoreA, newScoreB)
            }
        }
    }
}
