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

        val standingsList = mutableListOf<TeamStanding>()

        db.collection("tournaments").document(tournamentId!!).collection("standings")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("TableStatisticsFragment", "fetchTeamStandings success")
                for (document in documents) {
                    val standing = document.toObject(TeamStanding::class.java)
                    standingsList.add(standing)
                }

                // Sort standings by points, then goal difference
                standingsList.sortWith(
                    compareByDescending<TeamStanding> { it.points }
                        .thenByDescending { it.goals }
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
    }

    /**
     * Updates table data and refreshes the view.
     */
    fun updateTableStatistics(newData: List<TeamStanding>) {
        Log.d("TableStatisticsFragment", "updateTableStatistics called")

        teamStandings.clear()
        teamStandings.addAll(newData.sortedWith(
            compareByDescending<TeamStanding> { it.points }
                .thenByDescending { it.goals }
        ))
        statisticsAdapter.notifyDataSetChanged()
    }
}
