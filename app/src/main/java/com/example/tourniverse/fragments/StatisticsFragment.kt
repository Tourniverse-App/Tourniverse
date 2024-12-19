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
import com.example.tourniverse.adapters.StandingsAdapter
import com.example.tourniverse.adapters.StatisticsAdapter
import com.example.tourniverse.models.Match
import com.example.tourniverse.models.TeamStanding
import com.google.firebase.firestore.FirebaseFirestore

class StatisticsFragment : Fragment() {

    private lateinit var standingsRecyclerView: RecyclerView
    private lateinit var standingsAdapter: StandingsAdapter
    private val teamStandings = mutableListOf<TeamStanding>()
    private val db = FirebaseFirestore.getInstance()
    private var tournamentId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        // Initialize RecyclerView
        val statisticsRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewStatistics)
        statisticsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Fetch tournament type and data
        val isKnockout = arguments?.getBoolean("isKnockout") ?: false
        val tournamentId = arguments?.getString("tournamentId") ?: ""

        // Define collections
        val teamStatistics = mutableListOf<TeamStanding>()
        val knockoutMatches = mutableListOf<Match>()

        // Fetch data from Firestore
        fetchStatisticsData(tournamentId, isKnockout, teamStatistics, knockoutMatches) {
            val items = if (isKnockout) knockoutMatches else teamStatistics
            val statisticsAdapter = StatisticsAdapter(items = items, isKnockout = isKnockout)
            statisticsRecyclerView.adapter = statisticsAdapter
        }

        return view
    }

    private fun fetchStatisticsData(
        tournamentId: String,
        isKnockout: Boolean,
        teamStatistics: MutableList<TeamStanding>,
        knockoutMatches: MutableList<Match>,
        callback: () -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        if (isKnockout) {
            db.collection("tournaments").document(tournamentId).collection("matches")
                .get()
                .addOnSuccessListener { snapshot ->
                    knockoutMatches.clear()
                    for (document in snapshot.documents) {
                        val matchesArray = document.get("matches") as? List<Map<String, Any>> ?: continue
                        matchesArray.forEach { match ->
                            val teamA = match["teamA"] as? String ?: ""
                            val teamB = match["teamB"] as? String ?: ""
                            val scoreA = (match["scoreA"] as? Long)?.toInt() ?: 0
                            val scoreB = (match["scoreB"] as? Long)?.toInt() ?: 0
                            knockoutMatches.add(Match(teamA = teamA, teamB = teamB, scoreA = scoreA, scoreB = scoreB))
                        }
                    }
                    callback()
                }
        } else {
            db.collection("tournaments").document(tournamentId).collection("standings")
                .get()
                .addOnSuccessListener { snapshot ->
                    teamStatistics.clear()
                    for (document in snapshot.documents) {
                        val teamStat = document.toObject(TeamStanding::class.java)
                        teamStat?.let { teamStatistics.add(it) }
                    }
                    callback()
                }
        }
    }


    private fun calculateStandings(matchResults: List<Pair<String, String>>, tournamentId: String) {
        val standingsMap = mutableMapOf<String, TeamStanding>()

        matchResults.forEach { (team, result) ->
            val teamStanding = standingsMap.getOrDefault(team, TeamStanding(teamName = team))
            when (result) {
                "win" -> {
                    teamStanding.wins += 1
                    teamStanding.points += 3
                }
                "loss" -> teamStanding.losses += 1
                "draw" -> {
                    teamStanding.draws += 1
                    teamStanding.points += 1
                }
            }
            standingsMap[team] = teamStanding
        }

        teamStandings.clear()
        teamStandings.addAll(standingsMap.values.sortedByDescending { it.points })

        // Update Firestore with calculated standings
        val batch = db.batch()
        val standingsRef = db.collection("tournaments").document(tournamentId).collection("standings")
        standingsRef.get().addOnSuccessListener { documents ->
            documents.forEach { batch.delete(it.reference) }

            teamStandings.forEach { team ->
                val doc = standingsRef.document()
                batch.set(doc, team)
            }

            batch.commit()
                .addOnSuccessListener {
                    Log.d("StatisticsFragment", "Standings updated in Firestore.")
                    standingsAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("StatisticsFragment", "Failed to update standings in Firestore: ${e.message}")
                }
        }
    }
}
