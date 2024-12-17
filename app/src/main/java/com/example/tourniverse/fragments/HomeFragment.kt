package com.example.tourniverse.fragments

import android.os.Bundle
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

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_home, container, false)
//
//        // Initialize views
//        recyclerView = view.findViewById(R.id.recyclerTournaments)
//        noTournamentsView = view.findViewById(R.id.noTournamentsView)
//        val searchBar = view.findViewById<EditText>(R.id.searchBar)
//
//        // Setup RecyclerView
//        recyclerView.layoutManager = GridLayoutManager(context, 2)
//        adapter = TournamentAdapter(tournaments, ::navigateToTournamentDetails)
//        recyclerView.adapter = adapter
//
//        // Fetch tournaments
//        fetchUserTournaments()
//
//        // Search functionality
//        searchBar.addTextChangedListener(object : android.text.TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                adapter.filter(s.toString())
//            }
//            override fun afterTextChanged(s: android.text.Editable?) {}
//        })
//
//        return view
//    }

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


    private fun fetchUserTournaments() {
        FirebaseHelper.getUserTournaments { result ->
            if (result.isEmpty()) {
                recyclerView.visibility = View.GONE
                noTournamentsView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                noTournamentsView.visibility = View.GONE

                tournaments.clear()
                for (data in result) {
                    val name = data["name"] as? String ?: "Unknown"
                    val privacy = data["privacy"] as? String ?: "Private"
                    val description = data["description"] as? String ?: ""
                    val teamNames = data["teamNames"] as? List<String> ?: emptyList()
                    val ownerId = data["ownerId"] as? String ?: "Unknown"
                    val viewers = data["viewers"] as? List<String> ?: emptyList()

                    // Debug log
                    println("Tournament: $name, Privacy: $privacy, Teams: $teamNames, Owner: $ownerId")

                    val tournament = Tournament(
                        name = name,
                        type = privacy,
                        format = null, // Add a field if needed
                        description = description,
                        teamNames = teamNames,
                        owner = ownerId,
                        viewers = viewers
                    )
                    tournaments.add(tournament)
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun navigateToTournamentDetails(tournament: Tournament) {
        val bundle = Bundle().apply {
            putString("tournamentName", tournament.name)
            putString("tournamentType", tournament.type)
            putString("tournamentType", tournament.format) // my addition
        }
        findNavController().navigate(R.id.action_homeFragment_to_tournamentDetailsFragment, bundle)
    }
}
