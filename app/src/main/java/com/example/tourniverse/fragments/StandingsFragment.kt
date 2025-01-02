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
                .collection("matches").get()
                .addOnSuccessListener { documents ->
                    Log.d("StandingsFragment", "fetchFixtures success")
                    fixtures.clear()
                    for (document in documents) {
                        val matchesArray =
                            document.get("matches") as? List<Map<String, Any>> ?: continue
                        matchesArray.forEach { match ->
                            val teamA = match["teamA"] as? String ?: ""
                            val teamB = match["teamB"] as? String ?: ""
                            val scoreA = (match["scoreA"] as? Long)?.toInt() ?: 0
                            val scoreB = (match["scoreB"] as? Long)?.toInt() ?: 0
                            fixtures.add(Match(teamA, teamB, scoreA, scoreB))
                        }
                    }
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
        Log.d("StandingsFragment", "saveScoresToFirestore called")
        tournamentId?.let { id ->
            val batch = db.batch()
            db.collection("tournaments").document(id)
                .collection("matches").get()
                .addOnSuccessListener { documents ->
                    Log.d("StandingsFragment", "saveScoresToFirestore success")
                    for (document in documents) {
                        val matchesArray = document.get("matches") as? MutableList<Map<String, Any>>
                        if (matchesArray != null) {
                            matchesArray.forEachIndexed { index, match ->
                                val currentMatch = fixtures[index]
                                if (!(currentMatch.scoreA == 0 && currentMatch.scoreB == 0)) {
                                    matchesArray[index] = mapOf(
                                        "teamA" to currentMatch.teamA,
                                        "teamB" to currentMatch.teamB,
                                        "scoreA" to currentMatch.scoreA,
                                        "scoreB" to currentMatch.scoreB
                                    )
                                }
                            }

                            batch.update(
                                db.collection("tournaments").document(id)
                                    .collection("matches")
                                    .document(document.id),
                                "matches", matchesArray
                            )
                        }
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("StandingsFragment", "saveScoresToFirestore commit success")
                            updateStandings()
                            notifyStatisticsFragments()
                        }
                        .addOnFailureListener { e ->
                            Log.e("StandingsFragment", "saveScoresToFirestore commit failed: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("StandingsFragment", "saveScoresToFirestore failed: ${e.message}")
                }
        }
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
                0,
                0,
                0,
                match.scoreA,
                match.scoreA * 3 // Example point logic
            )
        })

        // Notify Knockout Statistics Fragment
        val knockoutFragment =
            parentFragmentManager.findFragmentByTag("knockoutStatisticsFragment") as? KnockoutStatisticsFragment
        knockoutFragment?.updateKnockoutMatches(fixtures)
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
            val scoreA = match.scoreA
            val scoreB = match.scoreB

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
