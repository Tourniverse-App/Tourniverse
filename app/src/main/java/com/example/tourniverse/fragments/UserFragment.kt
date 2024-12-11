package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TournamentAdapter
import com.example.tourniverse.models.Tournament
import androidx.navigation.fragment.findNavController


class UserFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        // Set User Details
        val userNameTextView = view.findViewById<TextView>(R.id.tvUserName)
        val profileImageView = view.findViewById<ImageView>(R.id.ivProfilePic)

        // Mock user data
        userNameTextView.text = "Netanel Baruch"
        profileImageView.setImageResource(R.drawable.ic_user)

        // RecyclerView setup for user's tournaments
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerUserTournaments)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val userTournaments = listOf(
            Tournament("טורניר כיתות ו׳", "Public"),
            Tournament("טורניר כיתות ג׳", "Private")
        )

        val adapter = TournamentAdapter(userTournaments) { tournament ->
            navigateToTournamentDetails(tournament)
        }

        recyclerView.adapter = adapter

        return view
    }

    private fun navigateToTournamentDetails(tournament: Tournament) {
        val bundle = Bundle().apply {
            putString("tournamentName", tournament.name)
            putString("tournamentType", tournament.type)
        }
        findNavController().navigate(R.id.action_userFragment_to_tournamentDetailsFragment, bundle)
    }
}
