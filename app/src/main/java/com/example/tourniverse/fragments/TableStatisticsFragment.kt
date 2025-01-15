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
    ): View {
        val view = inflater.inflate(R.layout.fragment_table_statistics, container, false)
        Log.d("TableStatisticsFragment", "onCreateView called")

        tournamentId = arguments?.getString("tournamentId").orEmpty()

        if (tournamentId.isNullOrEmpty()) {
            Log.e("TableStatisticsFragment", "Tournament ID is missing.")
            Toast.makeText(context, "Tournament ID is missing.", Toast.LENGTH_SHORT).show()
            return view
        }

        initializeRecyclerView(view)
        fetchTeamStandings()

        return view
    }

    private fun initializeRecyclerView(view: View) {
        statisticsRecyclerView = view.findViewById(R.id.recyclerViewTableStatistics)
        statisticsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        statisticsAdapter = TableStatisticsAdapter(teamStandings)
        statisticsRecyclerView.adapter = statisticsAdapter
    }

    private fun fetchTeamStandings() {
        tournamentId?.let { id ->
            Log.d("TableStatisticsFragment", "Fetching standings for tournament ID: $id")

            db.collection("tournaments").document(id)
                .collection("standings")
                .get()
                .addOnSuccessListener { documents ->
                    val standingsList = mutableListOf<TeamStanding>()

                    for (document in documents) {
                        val teamName = document.id
                        val wins = document.getLong("wins")?.toInt() ?: 0
                        val draws = document.getLong("draws")?.toInt() ?: 0
                        val losses = document.getLong("losses")?.toInt() ?: 0
                        val goals = document.getLong("goals")?.toInt() ?: 0
                        val points = document.getLong("points")?.toInt() ?: 0

                        standingsList.add(
                            TeamStanding(
                                teamName = teamName,
                                wins = wins,
                                draws = draws,
                                losses = losses,
                                goals = goals,
                                points = points
                            )
                        )
                    }

                    standingsList.sortWith(
                        compareByDescending<TeamStanding> { it.points }
                            .thenByDescending { it.goals }
                            .thenBy { it.teamName }
                    )

                    updateStandings(standingsList)
                }
                .addOnFailureListener { e ->
                    Log.e("TableStatisticsFragment", "Failed to fetch standings: ${e.message}")
                    Toast.makeText(context, "Failed to load standings.", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Log.e("TableStatisticsFragment", "Tournament ID is null!")
        }
    }

    fun updateStandings(newStandings: List<TeamStanding>) {
        teamStandings.clear()
        teamStandings.addAll(newStandings)
        statisticsAdapter.notifyDataSetChanged()
        Log.d("TableStatisticsFragment", "Standings updated. Total teams: ${teamStandings.size}")
    }

    override fun onResume() {
        super.onResume()
        Log.d("TableStatisticsFragment", "onResume called - refreshing standings")
        fetchTeamStandings()
    }
}
