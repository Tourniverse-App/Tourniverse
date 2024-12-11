package com.example.tourniverse.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TournamentAdapter
import com.example.tourniverse.models.Tournament

class HomeFragment : Fragment() {

    private lateinit var adapter: TournamentAdapter
    private lateinit var tournaments: MutableList<Tournament>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // RecyclerView setup
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTournaments)
        recyclerView.layoutManager = GridLayoutManager(context, 2) // Two items per row

        // Mocked data for tournaments
        tournaments = mutableListOf(
            Tournament("Tournament 1", "Public"),
            Tournament("Tournament 2", "Private"),
            Tournament("Tournament 3", "Public"),
            Tournament("Tournament 4", "Private"),
            Tournament("Tournament 5", "Public")
        )

        // Initialize Adapter
        adapter = TournamentAdapter(tournaments, ::navigateToTournamentDetails)
        recyclerView.adapter = adapter

        // Search Bar setup
        val searchBar = view.findViewById<EditText>(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                adapter.filter(query) // Filter tournaments based on query
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        return view
    }

    private fun navigateToTournamentDetails(tournament: Tournament) {
        val bundle = Bundle().apply {
            putString("tournamentName", tournament.name)
            putString("tournamentType", tournament.type)
        }
        findNavController().navigate(R.id.action_homeFragment_to_tournamentDetailsFragment, bundle)
    }
}
