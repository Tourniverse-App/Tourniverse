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
import com.example.tourniverse.adapters.TableStatisticsAdapter
import com.example.tourniverse.models.TeamStanding
import com.google.firebase.firestore.FirebaseFirestore

class TableStatisticsFragment : Fragment() {

    private lateinit var statisticsRecyclerView: RecyclerView
    private lateinit var statisticsAdapter: TableStatisticsAdapter
    private val teamStandings = mutableListOf<TeamStanding>()
    private val db = FirebaseFirestore.getInstance()
    private var tournamentId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("TableStatisticsFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_table_statistics, container, false)

        // Initialize RecyclerView
        statisticsRecyclerView = view.findViewById(R.id.recyclerViewTableStatistics)
        statisticsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize the adapter
        statisticsAdapter = TableStatisticsAdapter(teamStandings)
        statisticsRecyclerView.adapter = statisticsAdapter

        // Fetch arguments
        tournamentId = arguments?.getString("tournamentId") ?: ""

        Log.d("TableStatisticsFragment", "tournamentId: $tournamentId")

        if (tournamentId.isNullOrEmpty()) {
            Log.e("TableStatisticsFragment", "Tournament ID is missing.")
            Toast.makeText(context, "Tournament ID is missing.", Toast.LENGTH_SHORT).show()
            return view
        }

        // Fetch data
        fetchTeamStandings()

        return view
    }

    /**
     * Fetches team standings from Firestore and updates the adapter.
     */
    private fun fetchTeamStandings() {
        Log.d("TableStatisticsFragment", "fetchTeamStandings called")

        // Ensure tournamentId is not null
        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .collection("standings") // Fetch individual documents in the 'standings' collection
                .get()
                .addOnSuccessListener { documents ->
                    Log.d("TableStatisticsFragment", "fetchTeamStandings success")
                    val standingsList = mutableListOf<TeamStanding>()

                    // Process each document
                    for (document in documents) {
                        Log.d("TableStatisticsFragment", "Document ID: ${document.id}")

                        // Extract data based on correct keys
                        val teamName = document.id // Use the document ID as the team name
                        val wins = document.getLong("wins")?.toInt() ?: 0
                        val draws = document.getLong("draws")?.toInt() ?: 0
                        val losses = document.getLong("losses")?.toInt() ?: 0
                        val goals = document.getLong("goals")?.toInt() ?: 0
                        val points = document.getLong("points")?.toInt() ?: 0

                        Log.d(
                            "TableStatisticsFragment",
                            "Team: $teamName, Wins: $wins, Draws: $draws, Losses: $losses, Goals: $goals, Points: $points"
                        )

                        // Add team standing to the list
                        standingsList.add(
                            TeamStanding(
                                teamName = teamName, // Use document ID directly as team name
                                wins = wins,
                                draws = draws,
                                losses = losses,
                                goals = goals,
                                points = points
                            )
                        )
                    }

                    // Sort standings by points, then goal difference, then team name
                    standingsList.sortWith(
                        compareByDescending<TeamStanding> { it.points }
                            .thenByDescending { it.goals }
                            .thenBy { it.teamName } // Final fallback sort by team name
                    )

                    // Update the adapter with the sorted data
                    Log.d("TableStatisticsFragment", "Updating adapter with standings data")
                    teamStandings.clear()
                    teamStandings.addAll(standingsList)
                    statisticsAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("TableStatisticsFragment", "Failed to fetch team standings: ${e.message}")
                    Toast.makeText(context, "Failed to load standings.", Toast.LENGTH_SHORT).show()
                }
        } ?: Log.e("TableStatisticsFragment", "Tournament ID is null!")
    }


    /**
     * Updates table data and refreshes the view.
     */
    fun updateTableStatistics(newData: List<TeamStanding>) {
        Log.d("TableStatisticsFragment", "updateTableStatistics called")

        // Clear and update data
        teamStandings.clear()
        teamStandings.addAll(newData.sortedWith(
            compareByDescending<TeamStanding> { it.points }
                .thenByDescending { it.goals }
                .thenBy { it.teamName } // Sort by team name as fallback
        ))
        statisticsAdapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        Log.d("TableStatisticsFragment", "onResume called - refreshing standings")
        fetchTeamStandings() // Refresh standings when the user re-enters the page
    }

}
