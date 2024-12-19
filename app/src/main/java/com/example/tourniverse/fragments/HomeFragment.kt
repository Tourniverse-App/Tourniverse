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
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: TournamentAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var noTournamentsView: TextView
    private val tournaments = mutableListOf<Tournament>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        Log.d("HomeFragment", "onCreateView called")

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

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
     * Fetches tournaments from Firestore and populates the RecyclerView.
     */
    private fun fetchUserTournaments() {
        Log.d("HomeFragment", "Fetching tournaments from Firestore")

        db.collection("tournaments")
            .get()
            .addOnSuccessListener { querySnapshot ->
                tournaments.clear()
                if (querySnapshot.isEmpty) {
                    Log.d("HomeFragment", "No tournaments found")
                    recyclerView.visibility = View.GONE
                    noTournamentsView.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "No tournaments found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (document in querySnapshot.documents) {
                    val id = document.id
                    if (id.isNullOrEmpty()) {
                        Log.e("HomeFragment", "Missing tournament ID")
                        continue
                    }

                    val name = document.getString("name") ?: "Unknown"
                    val privacy = document.getString("privacy") ?: "Private"
                    val description = document.getString("description") ?: ""
                    val teamNames = try {
                        document.get("teamNames") as? List<String> ?: emptyList()
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error casting teamNames: ${e.message}")
                        emptyList<String>()
                    }
                    val ownerId = document.getString("ownerId") ?: "Unknown"

                    tournaments.add(
                        Tournament(
                            id = id,
                            name = name,
                            type = privacy,
                            description = description,
                            teamNames = teamNames,
                            owner = ownerId,
                            viewers = emptyList() // Adjust as needed
                        )
                    )
                }

                Log.d("HomeFragment", "Fetched ${tournaments.size} tournaments")
                recyclerView.visibility = View.VISIBLE
                noTournamentsView.visibility = View.GONE
                adapter.filter("")
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "${tournaments.size} tournaments loaded.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error fetching tournaments: ${e.message}")
                recyclerView.visibility = View.GONE
                noTournamentsView.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Failed to load tournaments.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Navigates to tournament details when a tournament is clicked.
     */
    private fun navigateToTournamentDetails(tournament: Tournament) {
        Log.d("HomeFragment", "Navigating to tournament details for ID: ${tournament.id}")
        Toast.makeText(requireContext(), "Opening tournament: ${tournament.name}", Toast.LENGTH_SHORT).show()

        val bundle = Bundle().apply {
            putString("tournamentId", tournament.id) // Pass the tournamentId
            putString("tournamentName", tournament.name)
            putString("tournamentType", tournament.type)
            putString("tournamentFormat", tournament.format) // Pass the format
            putString("tournamentDescription", tournament.description) // Pass the description
        }
        findNavController().navigate(R.id.action_homeFragment_to_tournamentDetailsFragment, bundle)
    }
}
