package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.StatisticsAdapter
import com.example.tourniverse.models.TeamStanding
import com.example.tourniverse.models.Match
import com.google.firebase.firestore.FirebaseFirestore

class StatisticsFragment : Fragment() {

    private lateinit var statisticsRecyclerView: RecyclerView
    private lateinit var knockoutLayout: LinearLayout
    private val teamStatistics = mutableListOf<TeamStanding>()
    private val knockoutMatches = mutableListOf<Match>()
    private lateinit var adapter: StatisticsAdapter
    private val db = FirebaseFirestore.getInstance()
    private var isKnockout: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        Log.d("StatisticsFragment", "onCreateView called")

        // Initialize views
        statisticsRecyclerView = view.findViewById(R.id.statisticsRecyclerView)
        knockoutLayout = view.findViewById(R.id.knockoutLayout)

        statisticsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = StatisticsAdapter(teamStatistics, isKnockout = false)
        statisticsRecyclerView.adapter = adapter

        val tournamentId = arguments?.getString("tournamentId")
        Log.d("StatisticsFragment", "Tournament ID from arguments: $tournamentId")

        if (tournamentId.isNullOrEmpty()) {
            Log.e("StatisticsFragment", "Tournament ID is missing")
            Toast.makeText(context, "Tournament ID is missing.", Toast.LENGTH_SHORT).show()
            return view
        }

        determineTournamentType(tournamentId)

        return view
    }

    private fun determineTournamentType(tournamentId: String) {
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    isKnockout = document.getString("type") == "Knockout"
                    Log.d("StatisticsFragment", "Tournament type isKnockout: $isKnockout")

                    if (isKnockout) {
                        fetchKnockoutMatches(tournamentId)
                        statisticsRecyclerView.visibility = View.GONE
                        knockoutLayout.visibility = View.VISIBLE
                    } else {
                        fetchTableStandings(tournamentId)
                        statisticsRecyclerView.visibility = View.VISIBLE
                        knockoutLayout.visibility = View.GONE
                    }
                } else {
                    Log.e("StatisticsFragment", "Tournament document not found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("StatisticsFragment", "Failed to determine tournament type: ${e.message}")
            }
    }

    private fun fetchTableStandings(tournamentId: String) {
        Log.d("StatisticsFragment", "Fetching table standings for Tournament ID: $tournamentId")

        db.collection("tournaments").document(tournamentId).collection("standings")
            .get()
            .addOnSuccessListener { snapshot ->
                teamStatistics.clear()
                for (document in snapshot.documents) {
                    val teamStat = document.toObject(TeamStanding::class.java)
                    teamStat?.let { teamStatistics.add(it) }
                }
                teamStatistics.sortByDescending { it.points }
                adapter.notifyDataSetChanged()
                Log.d("StatisticsFragment", "Standings fetched and sorted")
            }
            .addOnFailureListener { e ->
                Log.e("StatisticsFragment", "Failed to fetch standings: ${e.message}")
            }
    }

    private fun fetchKnockoutMatches(tournamentId: String) {
        Log.d("StatisticsFragment", "Fetching knockout matches for Tournament ID: $tournamentId")

        db.collection("tournaments").document(tournamentId).collection("knockout_bracket")
            .get()
            .addOnSuccessListener { snapshot ->
                knockoutMatches.clear()
                for (document in snapshot.documents) {
                    val match = document.toObject(Match::class.java)
                    match?.let { knockoutMatches.add(it) }
                }
                Log.d("StatisticsFragment", "Knockout matches fetched: ${knockoutMatches.size} items")
                // TODO: Update knockoutLayout dynamically based on fetched matches
            }
            .addOnFailureListener { e ->
                Log.e("StatisticsFragment", "Failed to fetch knockout matches: ${e.message}")
            }
    }
}
