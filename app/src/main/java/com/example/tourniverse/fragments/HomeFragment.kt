package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TournamentAdapter
import com.example.tourniverse.models.Tournament

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTournaments)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Mocked data for tournaments
        val tournaments = listOf(
            Tournament("Tournament 1", "Public"),
            Tournament("Tournament 2", "Private"),
            Tournament("Tournament 3", "Public")
        )

        val adapter = TournamentAdapter(tournaments)
        recyclerView.adapter = adapter

        return view
    }
}
