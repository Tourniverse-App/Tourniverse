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
import com.example.tourniverse.models.Match
import com.example.tourniverse.models.TeamStanding
import com.google.firebase.firestore.FirebaseFirestore

class StandingsFragment : Fragment() {

    private lateinit var standingsRecyclerView: RecyclerView
    private lateinit var fixturesRecyclerView: RecyclerView
    private lateinit var standingsAdapter: StandingsAdapter
    private lateinit var fixturesAdapter: StandingsAdapter

    private val db = FirebaseFirestore.getInstance()
    private var tournamentId: String? = null
    private val teamStandings = mutableListOf<TeamStanding>()
    private val fixtures = mutableListOf<Match>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_standings, container, false)

        Log.d("StandingsFragment", "onCreateView called")

        // Initialize RecyclerViews
        standingsRecyclerView = view.findViewById(R.id.recyclerViewStandings)
        fixturesRecyclerView = view.findViewById(R.id.recyclerViewFixtures)

        standingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        fixturesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        standingsAdapter = StandingsAdapter(teamStandings)
        fixturesAdapter = StandingsAdapter(fixtures)

        standingsRecyclerView.adapter = standingsAdapter
        fixturesRecyclerView.adapter = fixturesAdapter

        Log.d("StandingsFragment", "RecyclerViews and adapters initialized")

        // Fetch tournamentId from arguments
        tournamentId = arguments?.getString("tournamentId")
        Log.d("StandingsFragment", "Tournament ID from arguments: $tournamentId")

        if (tournamentId.isNullOrEmpty()) {
            Log.e("StandingsFragment", "Tournament ID is missing")
            Toast.makeText(context, "Tournament ID is missing.", Toast.LENGTH_SHORT).show()
            return view
        }

        fetchStandings()
        fetchFixtures()

        return view
    }

    private fun fetchStandings() {
        Log.d("StandingsFragment", "Fetching standings for Tournament ID: $tournamentId")

        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .collection("standings").get()
                .addOnSuccessListener { documents ->
                    teamStandings.clear()
                    for (document in documents) {
                        val team = document.toObject(TeamStanding::class.java)
                        teamStandings.add(team)
                    }
                    Log.d("StandingsFragment", "Standings fetched: ${teamStandings.size} items")
                    standingsAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("StandingsFragment", "Failed to load standings: ${e.message}")
                    Toast.makeText(context, "Failed to load standings: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: showTournamentIdError()
    }

    private fun fetchFixtures() {
        Log.d("StandingsFragment", "Fetching fixtures for Tournament ID: $tournamentId")

        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .collection("matches").get()
                .addOnSuccessListener { documents ->
                    fixtures.clear()
                    for (document in documents) {
                        val match = document.toObject(Match::class.java)
                        fixtures.add(match)
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

    private fun showTournamentIdError() {
        Log.e("StandingsFragment", "Invalid or missing tournament ID")
        Toast.makeText(context, "Invalid or missing tournament ID.", Toast.LENGTH_SHORT).show()
    }
}
