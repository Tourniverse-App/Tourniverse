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

class UserFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        // Set User Details
        val userNameTextView = view.findViewById<TextView>(R.id.tvUserName)
        val profileImageView = view.findViewById<ImageView>(R.id.ivProfilePic)

        // Mock user data for now
        userNameTextView.text = "John Doe"
        profileImageView.setImageResource(R.drawable.ic_user)

        // Setup RecyclerView for User's Tournaments
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerUserTournaments)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Mocked Tournament Data
        val userTournaments = listOf(
            Tournament("User Tournament 1", "Public"),
            Tournament("User Tournament 2", "Private")
        )
        recyclerView.adapter = TournamentAdapter(userTournaments)

        return view
    }
}
