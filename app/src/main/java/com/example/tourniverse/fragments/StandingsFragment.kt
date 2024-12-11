package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.MatchAdapter
import com.example.tourniverse.models.Match

class StandingsFragment : Fragment() {

    private lateinit var matchesRecyclerView: RecyclerView
    private val matches = mutableListOf<Match>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_standings, container, false)

        matchesRecyclerView = view.findViewById(R.id.matchesRecyclerView)
        matchesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Mock data for matches
        matches.add(Match("Team A", "Team B", "- : -"))
        matches.add(Match("Team C", "Team D", "- : -"))

        val adapter = MatchAdapter(matches)
        matchesRecyclerView.adapter = adapter

        // TODO: Add functionality to allow owners to update match scores

        return view
    }
}
