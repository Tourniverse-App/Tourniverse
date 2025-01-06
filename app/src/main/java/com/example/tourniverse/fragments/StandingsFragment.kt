package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.StandingsAdapter
import com.example.tourniverse.models.Match
import com.example.tourniverse.models.TeamStanding
import com.google.firebase.firestore.FirebaseFirestore

class StandingsFragment : Fragment() {

    private lateinit var fixturesRecyclerView: RecyclerView
    private lateinit var fixturesAdapter: StandingsAdapter
    private lateinit var saveButton: Button

    private val db = FirebaseFirestore.getInstance()
    private var tournamentId: String? = null
    private val fixtures = mutableListOf<Match>()
    private var isOwner = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("StandingsFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_standings, container, false)

        fixturesRecyclerView = view.findViewById(R.id.recyclerViewFixtures)
        saveButton = view.findViewById(R.id.saveButton)
        fixturesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Get arguments
        tournamentId = arguments?.getString("tournamentId")

        // Check if user is owner (compare user ID with tournament owner ID)
        isOwner = true // Replace this with actual ownership check logic

        Log.d("StandingsFragment", "tournamentId: $tournamentId, isOwner: $isOwner")

        // Show or hide save button based on ownership
        saveButton.visibility = if (isOwner) View.VISIBLE else View.GONE
        saveButton.setOnClickListener { saveScoresToFirestore() }

        fixturesAdapter = StandingsAdapter(fixtures) { match, newScoreA, newScoreB ->
            updateMatchScores(match, newScoreA, newScoreB)
        }
        fixturesRecyclerView.adapter = fixturesAdapter

        fetchFixtures()

        return view
    }

    /**
     * Fetches the fixtures (matches) from Firestore for the given tournament.
     */
    private fun fetchFixtures() {
        Log.d("StandingsFragment", "fetchFixtures called")

        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .collection("matches") // Fetch individual match documents
                .get()
                .addOnSuccessListener { documents ->
                    Log.d("StandingsFragment", "fetchFixtures success")
                    fixtures.clear()

                    for (document in documents) {
                        // Read match fields directly
                        val teamA = document.getString("teamA") ?: ""
                        val teamB = document.getString("teamB") ?: ""

                        // Handle scores safely by converting "-" to null
                        val scoreA = when (val rawScoreA = document.get("scoreA")) {
                            is String -> if (rawScoreA == "-") null else rawScoreA.toIntOrNull()
                            is Long -> rawScoreA.toInt()
                            else -> null
                        }

                        val scoreB = when (val rawScoreB = document.get("scoreB")) {
                            is String -> if (rawScoreB == "-") null else rawScoreB.toIntOrNull()
                            is Long -> rawScoreB.toInt()
                            else -> null
                        }

                        // Extract match ID
                        val matchId = document.id

                        Log.d("StandingsFragment", "Match: $teamA vs $teamB - Scores: $scoreA : $scoreB")

                        // Add match with ID to fixtures list
                        fixtures.add(Match(teamA, teamB, scoreA, scoreB, id = matchId))
                    }

                    // Notify adapter with updated data
                    fixturesAdapter.notifyDataSetChanged()
                    Log.d("StandingsFragment", "Fixtures updated in adapter")
                }
                .addOnFailureListener { e ->
                    Log.e("StandingsFragment", "fetchFixtures failed: ${e.message}")
                }
        }
    }


    /**
     * Saves the updated match scores to Firestore.
     */
    private fun saveScoresToFirestore() {
        Log.d("SaveButton", "Save button clicked.")

        tournamentId?.let { id ->
            val batch = db.batch()
            var hasInvalidScores = false

            fixtures.forEach { match ->
                val scoreA = match.scoreA
                val scoreB = match.scoreB

                Log.d("SaveButton", "Processing match: ${match.teamA} vs ${match.teamB} - Scores: $scoreA : $scoreB")

                // Skip matches with null in both fields
                if (scoreA == null && scoreB == null) {
                    Log.d("SaveButton", "Skipping match with both scores null.")
                    return@forEach
                }

                // Invalid cases: One field is null and the other is a number
                if ((scoreA == null && scoreB != null) || (scoreB == null && scoreA != null)) {
                    Log.d("SaveButton", "Invalid scores detected for match: ${match.teamA} vs ${match.teamB}")
                    hasInvalidScores = true
                    return@forEach
                }

                // Prepare valid updates for Firestore
                val matchData = mapOf(
                    "scoreA" to (scoreA ?: 0), // Default null to 0 instead of "-"
                    "scoreB" to (scoreB ?: 0)
                )

                // Ensure match.id exists before creating a document reference
                if (match.id.isNotEmpty()) {
                    val matchRef = db.collection("tournaments").document(id)
                        .collection("matches").document(match.id) // Use ID assigned earlier
                    batch.update(matchRef, matchData)
                    Log.d("SaveButton", "Added match to batch: ${match.teamA} vs ${match.teamB}")
                } else {
                    Log.e("SaveButton", "Match ID is missing for ${match.teamA} vs ${match.teamB}")
                    hasInvalidScores = true
                }
            }

            if (hasInvalidScores) {
                Toast.makeText(
                    context,
                    "Invalid scores found. Please fix them.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            batch.commit()
                .addOnSuccessListener {
                    Log.d("SaveButton", "Scores saved successfully.")
                    Toast.makeText(context, "Scores saved successfully.", Toast.LENGTH_SHORT).show()
                    notifyStatisticsFragments()
                }
                .addOnFailureListener { e ->
                    Log.e("SaveButton", "Failed to save scores: ${e.message}")
                    Toast.makeText(context, "Failed to save scores: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
        } ?: Log.e("SaveButton", "Tournament ID is null!")
    }


    /**
     * Updates standings for Table Statistics Fragment.
     */
    private fun notifyStatisticsFragments() {
        Log.d("StandingsFragment", "notifyStatisticsFragments called")

        // Notify Table Statistics Fragment
        val tableFragment =
            parentFragmentManager.findFragmentByTag("tableStatisticsFragment") as? TableStatisticsFragment
        tableFragment?.updateTableStatistics(fixtures.map { match ->
            TeamStanding(
                match.teamA,
                0, // Wins
                0, // Draws
                0, // Losses
                match.scoreA ?: 0, // Replace null with 0 for calculations
                (match.scoreA ?: 0) * 3 // Example point logic
            )
        })

        // Notify Knockout Statistics Fragment
        val knockoutFragment =
            parentFragmentManager.findFragmentByTag("knockoutStatisticsFragment") as? KnockoutStatisticsFragment
        knockoutFragment?.updateKnockoutMatches(fixtures.map { match ->
            // Replace null scores with 0 for knockout rounds
            match.copy(
                scoreA = match.scoreA ?: 0,
                scoreB = match.scoreB ?: 0
            )
        })
    }


    /**
     * Updates specific match scores in Firestore.
     */
    private fun updateMatchScores(match: Match, newScoreA: Int, newScoreB: Int) {
        Log.d("StandingsFragment", "updateMatchScores called for match: ${match.teamA} vs ${match.teamB}")
        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .collection("matches")
                .whereEqualTo("teamA", match.teamA)
                .whereEqualTo("teamB", match.teamB)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val matchRef = db.collection("tournaments")
                            .document(id)
                            .collection("matches")
                            .document(document.id)

                        matchRef.update("scoreA", newScoreA, "scoreB", newScoreB)
                            .addOnSuccessListener {
                                Log.d("StandingsFragment", "Match scores updated successfully.")
                                Toast.makeText(
                                    context,
                                    "Scores updated successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                notifyStatisticsFragments()
                            }
                            .addOnFailureListener { e ->
                                Log.e("StandingsFragment", "Failed to update match scores: ${e.message}")
                            }
                    }
                }
        }
    }

    /**
     * Updates the standings for each team based on the match results.
     */
    private fun updateStandings() {
        Log.d("StandingsFragment", "updateStandings called")
        val standingsMap = mutableMapOf<String, TeamStanding>()

        fixtures.forEach { match ->
            val teamA = match.teamA
            val teamB = match.teamB

            // Handle null scores by defaulting to 0
            val scoreA = match.scoreA ?: 0
            val scoreB = match.scoreB ?: 0

            val standingA = standingsMap.getOrDefault(teamA, TeamStanding(teamName = teamA))
            val standingB = standingsMap.getOrDefault(teamB, TeamStanding(teamName = teamB))

            standingA.goals += scoreA
            standingB.goals += scoreB

            when {
                scoreA > scoreB -> {
                    standingA.wins += 1
                    standingA.points += 3
                    standingB.losses += 1
                }
                scoreA < scoreB -> {
                    standingB.wins += 1
                    standingB.points += 3
                    standingA.losses += 1
                }
                else -> {
                    standingA.draws += 1
                    standingA.points += 1
                    standingB.draws += 1
                    standingB.points += 1
                }
            }

            standingsMap[teamA] = standingA
            standingsMap[teamB] = standingB
        }

        val batch = db.batch()
        val standingsRef = db.collection("tournaments").document(tournamentId!!).collection("standings")
        standingsMap.forEach { (teamName, standing) ->
            val docRef = standingsRef.document(teamName)
            batch.set(docRef, standing)
        }

        batch.commit()
            .addOnSuccessListener {
                Log.d("StandingsFragment", "updateStandings commit success")
            }
            .addOnFailureListener { e ->
                Log.e("StandingsFragment", "updateStandings commit failed: ${e.message}")
            }
    }


}
