package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.LinearLayout
import com.example.tourniverse.R
import com.example.tourniverse.adapters.KnockoutStatisticsAdapter
import com.example.tourniverse.models.Match
import com.google.firebase.firestore.FirebaseFirestore

class KnockoutStatisticsFragment : Fragment() {

    private lateinit var knockoutAdapter: KnockoutStatisticsAdapter
    private lateinit var bracketContainer: LinearLayout
    private val knockoutMatches = mutableListOf<Match>()
    private val db = FirebaseFirestore.getInstance()
    private var tournamentId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("KnockoutStatisticsFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_knockout_statistics, container, false)

        // Initialize container for brackets
        bracketContainer = view.findViewById(R.id.bracketContainer)

        // Fetch arguments
        tournamentId = arguments?.getString("tournamentId") ?: ""

        Log.d("KnockoutStatisticsFragment", "tournamentId: $tournamentId")

        if (tournamentId.isNullOrEmpty()) {
            Log.e("KnockoutStatisticsFragment", "Tournament ID is missing.")
            Toast.makeText(context, "Tournament ID is missing.", Toast.LENGTH_SHORT).show()
            return view
        }

        // Fetch data
        fetchKnockoutMatches()

        return view
    }

    /**
     * Fetches knockout matches from Firestore and updates the brackets.
     */
    private fun fetchKnockoutMatches() {
        Log.d("KnockoutStatisticsFragment", "fetchKnockoutMatches called")
        tournamentId?.let { id ->
            db.collection("tournaments").document(id).collection("matches")
                .get()
                .addOnSuccessListener { snapshot ->
                    Log.d("KnockoutStatisticsFragment", "fetchKnockoutMatches success")
                    knockoutMatches.clear()
                    for (document in snapshot.documents) {
                        val matchesArray = document.get("matches") as? List<Map<String, Any>> ?: continue
                        matchesArray.forEach { match ->
                            val teamA = match["teamA"] as? String ?: ""
                            val teamB = match["teamB"] as? String ?: ""
                            val scoreA = (match["scoreA"] as? Long)?.toInt() ?: 0
                            val scoreB = (match["scoreB"] as? Long)?.toInt() ?: 0

                            // Only add matches that have scores updated
                            if (!(scoreA == 0 && scoreB == 0)) {
                                knockoutMatches.add(Match(teamA, teamB, scoreA, scoreB))
                            }
                        }
                    }
                    displayKnockoutBrackets()
                }
                .addOnFailureListener { e ->
                    Log.e("KnockoutStatisticsFragment", "Failed to fetch knockout matches: ${e.message}")
                    Toast.makeText(context, "Failed to load matches.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Dynamically creates knockout brackets and updates the view.
     */
    private fun displayKnockoutBrackets() {
        Log.d("KnockoutStatisticsFragment", "displayKnockoutBrackets called")

        bracketContainer.removeAllViews()
        val inflater = LayoutInflater.from(context)

        var currentRound = knockoutMatches

        while (currentRound.isNotEmpty()) {
            val roundContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 16, 16, 16)
            }

            currentRound.forEach { match ->
                val matchView = inflater.inflate(R.layout.item_statistics_knockout, roundContainer, false)

                val matchTextView = matchView.findViewById<android.widget.TextView>(R.id.matchTextView)
                matchTextView.text = "${match.teamA} vs ${match.teamB}\n${match.scoreA} : ${match.scoreB}"

                roundContainer.addView(matchView)
            }

            bracketContainer.addView(roundContainer)

            // Prepare next round
            val nextRound = mutableListOf<Match>()
            for (i in 0 until currentRound.size step 2) {
                if (i + 1 < currentRound.size) {
                    val winnerA = if (currentRound[i].scoreA > currentRound[i].scoreB) currentRound[i].teamA else currentRound[i].teamB
                    val winnerB = if (currentRound[i + 1].scoreA > currentRound[i + 1].scoreB) currentRound[i + 1].teamA else currentRound[i + 1].teamB
                    nextRound.add(Match(winnerA, winnerB, 0, 0))
                }
            }
            currentRound = nextRound
        }
        Log.d("KnockoutStatisticsFragment", "Knockout brackets displayed successfully.")
    }

    /**
     * Updates knockout data and refreshes the view.
     */
    fun updateKnockoutMatches(newMatches: List<Match>) {
        Log.d("KnockoutStatisticsFragment", "updateKnockoutMatches called")

        knockoutMatches.clear()
        knockoutMatches.addAll(newMatches)
        displayKnockoutBrackets()
    }
}
