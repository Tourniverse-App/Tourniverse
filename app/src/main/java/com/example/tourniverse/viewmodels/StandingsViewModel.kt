package com.example.tourniverse.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.tourniverse.adapters.StandingsAdapter
import com.example.tourniverse.fragments.TableStatisticsFragment
import com.example.tourniverse.models.Match
import com.example.tourniverse.models.TeamStanding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StandingsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    val fixtures = mutableListOf<Match>()

    fun getCurrentUserId(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser?.uid ?: "" // Returns the user's UID or an empty string if not logged in
    }

    fun fetchFixtures(tournamentId: String, fixturesAdapter: StandingsAdapter) {
        val TAG = "fetchViewModel"

        Log.d(TAG, "fetchFixtures called with tournamentId: $tournamentId")

        db.collection("tournaments").document(tournamentId)
            .collection("matches") // Fetch individual match documents
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "fetchFixtures success, documents size: ${documents.size()}")
                fixtures.clear()

                for (document in documents) {
                    Log.d(TAG, "Processing document: ${document.id}")

                    // Read match fields directly
                    val teamA = document.getString("teamA") ?: ""
                    val teamB = document.getString("teamB") ?: ""
                    Log.d(TAG, "Teams: $teamA vs $teamB")

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
                    Log.d(TAG, "Scores: $scoreA : $scoreB")

                    // Extract match ID
                    val matchId = document.id
                    Log.d(TAG, "Match ID: $matchId")

                    // Add match with ID to fixtures list
                    fixtures.add(Match(teamA, teamB, scoreA, scoreB, id = matchId))
                }

                // Notify adapter with updated data
                fixturesAdapter.notifyDataSetChanged()
                Log.d(TAG, "Fixtures updated in adapter, total fixtures: ${fixtures.size}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "fetchFixtures failed: ${e.message}")
            }
    }

    fun notifyStatisticsFragments(
        tableFragment: TableStatisticsFragment?,
        format: String
    ) {
        Log.d("StandingsFragment", "notifyStatisticsFragments called")

        if (format == "Tables") {
            // Notify Table Statistics Fragment
            if (tableFragment != null) {
                Log.d("StandingsFragment", "Updating Table Statistics Fragment")
                val tableStandings = fixtures.map { match ->
                    TeamStanding(
                        teamName = match.teamA,
                        wins = 0, // Placeholder logic for wins
                        draws = 0, // Placeholder logic for draws
                        losses = 0, // Placeholder logic for losses
                        goals = match.scoreA ?: 0, // Replace null with 0 for goals
                        points = (match.scoreA ?: 0) * 3 // Example point logic
                    )
                }
                tableFragment.updateStandings(tableStandings) // Updated function name
            } else {
                Log.e("StandingsFragment", "TableStatisticsFragment not found or not initialized")
            }
        }
    }
}