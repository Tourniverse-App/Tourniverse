package com.example.tourniverse.fragments

import android.os.Bundle
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
import com.example.tourniverse.models.TeamStatistics
import com.google.firebase.firestore.FirebaseFirestore

class StatisticsFragment : Fragment() {

    private lateinit var statisticsRecyclerView: RecyclerView
    private val teamStatistics = mutableListOf<TeamStanding>()
    private lateinit var adapter: StatisticsAdapter
    private val db = FirebaseFirestore.getInstance()
    private val teamStatsCollection = db.collection("tournaments").document("yourTournamentId").collection("standings")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        // Initialize RecyclerView
        statisticsRecyclerView = view.findViewById(R.id.statisticsRecyclerView)
        statisticsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = StatisticsAdapter(teamStatistics)
        statisticsRecyclerView.adapter = adapter

        fetchTeamStatistics()

        return view
    }

    private fun fetchTeamStatistics() {
        teamStatsCollection.get()
            .addOnSuccessListener { snapshot ->
                teamStatistics.clear()
                for (document in snapshot.documents) {
                    val teamStat = document.toObject(TeamStanding::class.java)
                    teamStat?.let { teamStatistics.add(it) }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load statistics: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
