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

        // Initialize RecyclerViews
        standingsRecyclerView = view.findViewById(R.id.recyclerViewStandings)
        fixturesRecyclerView = view.findViewById(R.id.recyclerViewFixtures)

        standingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        fixturesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        standingsAdapter = StandingsAdapter(teamStandings)
        fixturesAdapter = StandingsAdapter(fixtures)

        standingsRecyclerView.adapter = standingsAdapter
        fixturesRecyclerView.adapter = fixturesAdapter

        // Fetch tournamentId from arguments
        tournamentId = arguments?.getString("tournamentId")
        if (tournamentId.isNullOrEmpty()) {
            Toast.makeText(context, "Tournament ID is missing.", Toast.LENGTH_SHORT).show()
            return view
        }

        fetchStandings()
        fetchFixtures()

        return view
    }

    private fun fetchStandings() {
        db.collection("tournaments").document(tournamentId!!)
            .collection("standings").get()
            .addOnSuccessListener { documents ->
                teamStandings.clear()
                for (document in documents) {
                    val team = document.toObject(TeamStanding::class.java)
                    teamStandings.add(team)
                }
                standingsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load standings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchFixtures() {
        db.collection("tournaments").document(tournamentId!!)
            .collection("matches").get()
            .addOnSuccessListener { documents ->
                fixtures.clear()
                for (document in documents) {
                    val match = document.toObject(Match::class.java)
                    fixtures.add(match)
                }
                fixturesAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load fixtures: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
