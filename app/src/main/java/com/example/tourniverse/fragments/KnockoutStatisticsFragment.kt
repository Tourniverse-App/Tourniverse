package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tourniverse.R
import com.example.tourniverse.models.Match
import com.example.tourniverse.viewmodels.KnockoutStatisticsViewModel

class KnockoutStatisticsFragment : Fragment() {

    private lateinit var viewModel: KnockoutStatisticsViewModel
    private lateinit var bracketContainer: LinearLayout
    private var tournamentId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("KnockoutStatisticsFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_knockout_statistics, container, false)

        viewModel = ViewModelProvider(this).get(KnockoutStatisticsViewModel::class.java)

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
        tournamentId?.let {
            viewModel.fetchKnockoutMatches(
                it,
                onSuccess = { matches ->
                    updateKnockoutMatches(matches)
                },
                onError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            )
        }

        return view
    }

    private fun displayKnockoutBrackets() {
        Log.d("KnockoutStatisticsFragment", "displayKnockoutBrackets called")

        // Clear the bracket container before adding new views
        bracketContainer.removeAllViews()
        val inflater = LayoutInflater.from(context)

        // Start with the first round of matches
        var currentRound = viewModel.knockoutMatches

        // Loop through each round until there are no more matches
        while (currentRound.isNotEmpty()) {
            val roundContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 16, 16, 16)
            }

            // Create views for each match in the current round
            currentRound.forEach { match ->
                val matchView = inflater.inflate(R.layout.item_statistics_knockout, roundContainer, false)

                val matchTextView = matchView.findViewById<android.widget.TextView>(R.id.matchTextView)

                // Display scores, default to "-" if null
                matchTextView.text = "${match.teamA} vs ${match.teamB}\n${match.scoreA ?: "-"} : ${match.scoreB ?: "-"}"

                roundContainer.addView(matchView)
            }

            // Add the round container to the main bracket container
            bracketContainer.addView(roundContainer)

            // Prepare the next round by calculating winners
            val nextRound = mutableListOf<Match>()
            for (i in 0 until currentRound.size step 2) {
                if (i + 1 < currentRound.size) {
                    // Determine winners based on scores, defaulting to 0 if null
                    val winnerA = if ((currentRound[i].scoreA ?: 0) > (currentRound[i].scoreB ?: 0))
                        currentRound[i].teamA
                    else
                        currentRound[i].teamB

                    val winnerB = if ((currentRound[i + 1].scoreA ?: 0) > (currentRound[i + 1].scoreB ?: 0))
                        currentRound[i + 1].teamA
                    else
                        currentRound[i + 1].teamB

                    // Create a new match for the next round with default scores
                    nextRound.add(Match(winnerA, winnerB, null, null))
                }
            }

            // Move to the next round
            currentRound = nextRound
        }
        Log.d("KnockoutStatisticsFragment", "Knockout brackets displayed successfully.")
    }

    fun updateKnockoutMatches(newMatches: List<Match>) {
        Log.d("KnockoutStatisticsFragment", "updateKnockoutMatches called")
        displayKnockoutBrackets()
    }
}
