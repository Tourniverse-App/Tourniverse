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
import com.example.tourniverse.adapters.StatisticsAdapter
import com.example.tourniverse.models.TeamStanding
import com.google.firebase.firestore.FirebaseFirestore

class StatisticsFragment : Fragment() {

    private lateinit var statisticsRecyclerView: RecyclerView
    private val teamStatistics = mutableListOf<TeamStanding>()
    private lateinit var adapter: StatisticsAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        Log.d("StatisticsFragment", "onCreateView called")

        // Initialize RecyclerView
        statisticsRecyclerView = view.findViewById(R.id.statisticsRecyclerView)
        statisticsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = StatisticsAdapter(teamStatistics)
        statisticsRecyclerView.adapter = adapter

        Log.d("StatisticsFragment", "RecyclerView and adapter initialized")

        val tournamentId = arguments?.getString("tournamentId")
        Log.d("StatisticsFragment", "Tournament ID from arguments: $tournamentId")

        if (tournamentId.isNullOrEmpty()) {
            Log.e("StatisticsFragment", "Tournament ID is missing")
            Toast.makeText(context, "Tournament ID is missing.", Toast.LENGTH_SHORT).show()
            return view
        }

        fetchTeamStatistics(tournamentId)

        return view
    }

    private fun fetchTeamStatistics(tournamentId: String) {
        Log.d("StatisticsFragment", "Fetching team statistics for Tournament ID: $tournamentId")

        db.collection("tournaments").document(tournamentId).collection("standings")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.e("StatisticsFragment", "No team statistics found for tournamentId: $tournamentId")
                    Toast.makeText(context, "No statistics available.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                teamStatistics.clear()
                for (document in snapshot.documents) {
                    val teamStat = document.toObject(TeamStanding::class.java)
                    teamStat?.let { teamStatistics.add(it) }
                }
                Log.d("StatisticsFragment", "Team statistics fetched: ${teamStatistics.size} items")
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("StatisticsFragment", "Failed to fetch team statistics: ${e.message}")
                Toast.makeText(context, "Failed to load statistics: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
