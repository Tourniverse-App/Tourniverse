package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.FixturesAdapter // Replace with the correct adapter
import com.example.tourniverse.models.Match
import com.google.firebase.firestore.FirebaseFirestore

class StandingsFragment : Fragment() {

    private lateinit var fixturesRecyclerView: RecyclerView
    private lateinit var fixturesAdapter: FixturesAdapter // Updated Adapter

    private val db = FirebaseFirestore.getInstance()
    private var tournamentId: String? = null
    private val fixtures = mutableListOf<Match>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_standings, container, false)

        Log.d("StandingsFragment", "onCreateView called")

        // Initialize RecyclerView
        fixturesRecyclerView = view.findViewById(R.id.recyclerViewFixtures)
        fixturesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize the updated FixturesAdapter
        fixturesAdapter = FixturesAdapter(
            fixtures,
            tournamentId = tournamentId ?: ""
        ) { match, newScoreA, newScoreB ->
            updateMatchScores(match, newScoreA, newScoreB) // Pass the lambda for score updates
        }
        fixturesRecyclerView.adapter = fixturesAdapter

        Log.d("StandingsFragment", "RecyclerView and adapter initialized")

        // Fetch tournamentId from arguments
        tournamentId = arguments?.getString("tournamentId")
        Log.d("StandingsFragment", "Tournament ID from arguments: $tournamentId")

        if (tournamentId.isNullOrEmpty()) {
            Log.e("StandingsFragment", "Tournament ID is missing")
            Toast.makeText(context, "Tournament ID is missing.", Toast.LENGTH_SHORT).show()
            return view
        }

        fetchFixtures()

        return view
    }


    private fun fetchFixtures() {
        Log.d("StandingsFragment", "Fetching fixtures for Tournament ID: $tournamentId")

        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .collection("matches").get()
                .addOnSuccessListener { documents ->
                    fixtures.clear()
                    for (document in documents) {
                        val matchesArray = document.get("matches") as? List<Map<String, Any>>
                        matchesArray?.forEach { match ->
                            val teamA = match["teamA"] as? String ?: ""
                            val teamB = match["teamB"] as? String ?: ""
                            val scoreA = (match["scoreA"] as? Long)?.toInt() ?: 0
                            val scoreB = (match["scoreB"] as? Long)?.toInt() ?: 0
                            fixtures.add(Match(teamA = teamA, teamB = teamB, scoreA = scoreA, scoreB = scoreB))
                        }
                    }

                    Log.d("StandingsFragment", "Fixtures fetched: ${fixtures.size} items")
                    fixturesAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("StandingsFragment", "Failed to load fixtures: ${e.message}")
                    Toast.makeText(context, "Failed to load fixtures: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: showTournamentIdError()
    }

    private fun updateMatchScores(match: Match, newScoreA: Int, newScoreB: Int) {
        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .collection("matches").get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val matchesArray = document.get("matches") as? MutableList<Map<String, Any>>
                        if (matchesArray != null) {
                            val matchIndex = matchesArray.indexOfFirst {
                                it["teamA"] == match.teamA && it["teamB"] == match.teamB
                            }
                            if (matchIndex != -1) {
                                matchesArray[matchIndex] = mapOf(
                                    "teamA" to match.teamA,
                                    "teamB" to match.teamB,
                                    "scoreA" to newScoreA,
                                    "scoreB" to newScoreB
                                )
                                db.collection("tournaments").document(id)
                                    .collection("matches")
                                    .document(document.id)
                                    .update("matches", matchesArray)
                                    .addOnSuccessListener {
                                        Log.d("StandingsFragment", "Scores updated for match: $match")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("StandingsFragment", "Failed to update scores: ${e.message}")
                                    }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("StandingsFragment", "Failed to load matches for update: ${e.message}")
                }
        }
    }

    private fun showTournamentIdError() {
        Log.e("StandingsFragment", "Invalid or missing tournament ID")
        Toast.makeText(context, "Invalid or missing tournament ID.", Toast.LENGTH_SHORT).show()
    }
}
