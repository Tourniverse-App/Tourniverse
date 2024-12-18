package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TournamentAdapter
import com.example.tourniverse.models.Tournament
import com.example.tourniverse.utils.FirebaseHelper

class HomeFragment : Fragment() {

    private lateinit var adapter: TournamentAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var noTournamentsView: TextView
    private val tournaments = mutableListOf<Tournament>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerTournaments)
        noTournamentsView = view.findViewById(R.id.noTournamentsView)
        val searchBar = view.findViewById<EditText>(R.id.searchBar)

        // Setup RecyclerView with GridLayoutManager as in XML
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        adapter = TournamentAdapter(tournaments, ::navigateToTournamentDetails)
        recyclerView.adapter = adapter

        // Fetch tournaments
        fetchUserTournaments()

        // Attach search functionality
        searchBar.addTextChangedListener(adapter.getSearchTextWatcher())

        return view
    }


    /**
     * Fetches both owned and viewed tournaments for the current user.
     * Populates the RecyclerView with all tournaments when the app starts.
     */
    private fun fetchUserTournaments() {
        FirebaseHelper.getUserTournaments(includeViewed = true) { result ->
            recyclerView.visibility = View.VISIBLE
            noTournamentsView.visibility = View.GONE

            tournaments.clear()
            result.forEach { data ->
                val name = data["name"] as? String ?: "Unknown"
                val privacy = data["privacy"] as? String ?: "Private"
                val description = data["description"] as? String ?: ""
                val teamNames = data["teamNames"] as? List<String> ?: emptyList()
                val ownerId = data["ownerId"] as? String ?: "Unknown"
                val viewers = data["viewers"] as? List<String> ?: emptyList()

                val tournament = Tournament(
                    name = name,
                    type = privacy,
                    description = description,
                    teamNames = teamNames,
                    owner = ownerId,
                    viewers = viewers
                )
                tournaments.add(tournament)
            }

            adapter.filter("") // Show all tournaments initially
            adapter.notifyDataSetChanged()
        }
    }



    private fun navigateToTournamentDetails(tournament: Tournament) {
        val bundle = Bundle().apply {
            putString("tournamentName", tournament.name)
            putString("tournamentType", tournament.type)
            putString("tournamentFormat", tournament.format) // Pass the format
            putString("tournamentDescription", tournament.description) // Pass the description
        }
        findNavController().navigate(R.id.action_homeFragment_to_tournamentDetailsFragment, bundle)
    }

}