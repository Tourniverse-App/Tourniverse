package com.example.tourniverse.adapters

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.Match
import com.google.firebase.firestore.FirebaseFirestore

class FixturesAdapter(
    private val fixtures: MutableList<Match>,
    private val tournamentId: String,
    private val updateScoresCallback: (Match, Int, Int) -> Unit
) : RecyclerView.Adapter<FixturesAdapter.FixtureViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FixtureViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_standing, parent, false)
        return FixtureViewHolder(view)
    }

    override fun onBindViewHolder(holder: FixtureViewHolder, position: Int) {
        val match = fixtures[position]

        holder.teamAName.text = match.teamA
        holder.teamBName.text = match.teamB
        holder.scoreA.setText(match.scoreA.toString())
        holder.scoreB.setText(match.scoreB.toString())

        // Add listeners for scores
        holder.scoreA.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                try {
                    val updatedScoreA = s?.toString()?.toIntOrNull() ?: 0
                    Log.d("FixturesAdapter", "ScoreA updated to $updatedScoreA for match: ${match.teamA} vs ${match.teamB}")
                    updateMatchScores(match, updatedScoreA, match.scoreB)
                } catch (e: Exception) {
                    Log.e("FixturesAdapter", "Error in afterTextChanged for ScoreA: ${e.message}")
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("FixturesAdapter", "beforeTextChanged triggered for ScoreA. Input: $s")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d("FixturesAdapter", "onTextChanged triggered for ScoreA. Input: $s")
            }
        })

        holder.scoreB.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                try {
                    val updatedScoreB = s?.toString()?.toIntOrNull() ?: 0
                    Log.d("FixturesAdapter", "ScoreB updated to $updatedScoreB for match: ${match.teamA} vs ${match.teamB}")
                    updateMatchScores(match, match.scoreA, updatedScoreB)
                } catch (e: Exception) {
                    Log.e("FixturesAdapter", "Error in afterTextChanged for ScoreB: ${e.message}")
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("FixturesAdapter", "beforeTextChanged triggered for ScoreB. Input: $s")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d("FixturesAdapter", "onTextChanged triggered for ScoreB. Input: $s")
            }
        })

    }

    private fun updateMatchScores(match: Match, newScoreA: Int, newScoreB: Int) {
        Log.d("StandingsAdapter", "Updating scores: $newScoreA : $newScoreB for match: ${match.teamA} vs ${match.teamB}")
        if (match.id.isNullOrEmpty()) {
            Log.e("StandingsAdapter", "Match ID is null or empty. Cannot update scores.")
            return
        }

        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .collection("matches").document(match.id!!)
                .update(
                    mapOf(
                        "scoreA" to newScoreA,
                        "scoreB" to newScoreB
                    )
                )
                .addOnSuccessListener {
                    Log.d("StandingsAdapter", "Scores successfully updated in Firestore.")
                }
                .addOnFailureListener { e ->
                    Log.e("StandingsAdapter", "Failed to update scores in Firestore: ${e.message}")
                }
        } ?: Log.e("StandingsAdapter", "Tournament ID is null. Cannot update scores.")
    }



    override fun getItemCount() = fixtures.size

    class FixtureViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val teamAName: TextView = view.findViewById(R.id.teamAName)
        val teamBName: TextView = view.findViewById(R.id.teamBName)
        val scoreA: EditText = view.findViewById(R.id.scoreA)
        val scoreB: EditText = view.findViewById(R.id.scoreB)
    }
}
