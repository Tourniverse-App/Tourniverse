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
import com.example.tourniverse.models.Match
import com.example.tourniverse.models.TeamStanding
import com.google.firebase.firestore.FirebaseFirestore

class StatisticsFragment : Fragment() {

    private lateinit var statisticsRecyclerView: RecyclerView
    private lateinit var statisticsAdapter: StatisticsAdapter
    private val teamStandings = mutableListOf<TeamStanding>()
    private val knockoutMatches = mutableListOf<Match>()
    private val db = FirebaseFirestore.getInstance()
    private var tournamentId: String? = null
    private var isKnockout: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("StatisticsFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        // Initialize RecyclerView
        statisticsRecyclerView = view.findViewById(R.id.recyclerViewStatistics)
        statisticsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize the adapter
        statisticsAdapter = StatisticsAdapter(teamStandings, false)
        statisticsRecyclerView.adapter = statisticsAdapter

        // Fetch arguments
        isKnockout = arguments?.getBoolean("isKnockout") ?: false
        tournamentId = arguments?.getString("tournamentId") ?: ""

        Log.d("StatisticsFragment", "isKnockout: $isKnockout, tournamentId: $tournamentId")

        if (tournamentId.isNullOrEmpty()) {
            Log.e("StatisticsFragment", "Tournament ID is missing.")
            Toast.makeText(context, "Tournament ID is missing.", Toast.LENGTH_SHORT).show()
            return view
        }

        // Fetch data
        fetchStatisticsData()

        return view
    }

    fun fetchStatisticsData() {
        Log.d("StatisticsFragment", "Fetching statistics data for tournament: $tournamentId")

        if (isKnockout) {
            fetchKnockoutMatches()
        } else {
            fetchTeamStandings()
        }
    }

    private fun fetchKnockoutMatches() {
        Log.d("StatisticsFragment", "fetchKnockoutMatches called")
        tournamentId?.let { id ->
            db.collection("tournaments").document(id).collection("matches")
                .get()
                .addOnSuccessListener { snapshot ->
                    Log.d("StatisticsFragment", "fetchKnockoutMatches success")
                    knockoutMatches.clear()
                    for (document in snapshot.documents) {
                        val matchesArray = document.get("matches") as? List<Map<String, Any>> ?: continue
                        matchesArray.forEach { match ->
                            val teamA = match["teamA"] as? String ?: ""
                            val teamB = match["teamB"] as? String ?: ""
                            val scoreA = (match["scoreA"] as? Long)?.toInt() ?: 0
                            val scoreB = (match["scoreB"] as? Long)?.toInt() ?: 0

                            // Only add matches that have scores updated
                            if (!(scoreA == 0 && scoreB == 0)) {
                                knockoutMatches.add(Match(teamA, teamB, scoreA, scoreB))
                            }
                        }
                    }
                    updateKnockoutRounds()
                }
                .addOnFailureListener { e ->
                    Log.e("StatisticsFragment", "Failed to fetch knockout matches: ${e.message}")
                }
        }
    }

    private fun fetchTeamStandings() {
        Log.d("StatisticsFragment", "fetchTeamStandings called")

        val standingsList = mutableListOf<TeamStanding>()

        db.collection("tournaments").document(tournamentId!!).collection("standings")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("StatisticsFragment", "fetchTeamStandings success")
                for (document in documents) {
                    val standing = document.toObject(TeamStanding::class.java)
                    standingsList.add(standing)
                }

                // Sort standings by points, then goal difference
                standingsList.sortWith(compareByDescending<TeamStanding> { it.points }
                    .thenByDescending { it.goals })

                // Update the adapter with the sorted data
                Log.d("StatisticsFragment", "Updating adapter with standings data")
                statisticsAdapter.updateData(standingsList)
            }
            .addOnFailureListener { e ->
                Log.e("StatisticsFragment", "Failed to fetch team standings: ${e.message}")
            }
    }

    private fun updateKnockoutRounds() {
        Log.d("StatisticsFragment", "updateKnockoutRounds called")

        // Simulate advancing winners
        val nextRound = mutableListOf<Match>()
        for (i in 0 until knockoutMatches.size step 2) {
            val match1 = knockoutMatches[i]
            val winner = if (match1.scoreA > match1.scoreB) match1.teamA else match1.teamB

            if (i + 1 < knockoutMatches.size) {
                val match2 = knockoutMatches[i + 1]
                val winner2 = if (match2.scoreA > match2.scoreB) match2.teamA else match2.teamB
                nextRound.add(Match(winner, winner2, 0, 0)) // Next round match
            }
        }

        knockoutMatches.addAll(nextRound)

        Log.d("StatisticsFragment", "Updating adapter with knockout matches data")
        statisticsAdapter = StatisticsAdapter(knockoutMatches, true)
        statisticsRecyclerView.adapter = statisticsAdapter
        statisticsAdapter.notifyDataSetChanged()
    }

    private fun updateTableStatistics() {
        Log.d("StatisticsFragment", "updateTableStatistics called")

        statisticsAdapter = StatisticsAdapter(teamStandings, false)
        statisticsRecyclerView.adapter = statisticsAdapter
        statisticsAdapter.notifyDataSetChanged()
    }

    private fun calculateStandings(matchResults: List<Pair<String, String>>) {
        Log.d("StatisticsFragment", "calculateStandings called")
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
        val standingsRef = db.collection("tournaments").document(tournamentId!!).collection("standings")
        standingsRef.get().addOnSuccessListener { documents ->
            documents.forEach { batch.delete(it.reference) }

            teamStandings.forEach { team ->
                val doc = standingsRef.document()
                batch.set(doc, team)
            }

            batch.commit()
                .addOnSuccessListener {
                    Log.d("StatisticsFragment", "Standings updated in Firestore.")
                    statisticsAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("StatisticsFragment", "Failed to update standings in Firestore: ${e.message}")
                }
        }
    }
}