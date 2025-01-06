package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TournamentPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A fragment that displays the details of a tournament.
 * It fetches the tournament details from Firestore and updates the UI.
 * It also sets up a ViewPager2 with a TabLayout for different sections of the tournament.
 */
class TournamentDetailsFragment : Fragment() {

    private var tournamentId: String? = null
    private lateinit var tvTournamentName: TextView
    private lateinit var tvTournamentType: TextView
    private lateinit var tvTournamentFormat: TextView
    private lateinit var tvTournamentDescription: TextView
    private lateinit var btnViewStatistics: Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get tournamentId from arguments
        tournamentId = arguments?.getString("tournamentId")
        if (tournamentId.isNullOrEmpty()) {
            Log.e("TournamentDetails", "Tournament ID is null or empty.")
            Toast.makeText(requireContext(), "Invalid tournament ID.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tournament_details, container, false)

        // Initialize Views
        tvTournamentName = view.findViewById(R.id.tvTournamentName)
        tvTournamentType = view.findViewById(R.id.tvTournamentType)
        tvTournamentFormat = view.findViewById(R.id.tvTournamentFormat)
        tvTournamentDescription = view.findViewById(R.id.tvTournamentDescription)
        btnViewStatistics = view.findViewById(R.id.btnViewStatistics)

        // Fetch tournament details if tournamentId is valid
        if (!tournamentId.isNullOrEmpty()) {
            fetchTournamentDetails()
        }

        // Setup TabLayout and ViewPager2
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val adapter = TournamentPagerAdapter(requireActivity(), tournamentId ?: "", "Tables") // Pass the tournamentFormat

        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Social"
                1 -> "Fixtures"
                2 -> "Statistics"
                3 -> "Settings"
                else -> "Tab $position"
            }
        }.attach()

        return view
    }

    private fun fetchTournamentDetails() {
        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "Unknown Tournament"
                        val privacy = document.getString("privacy") ?: "Unknown"
                        val description = document.getString("description") ?: ""
                        val format = document.getString("format") ?: "Unknown"

                        // Set details in UI
                        tvTournamentName.text = name
                        tvTournamentType.text = "Type: $privacy"
                        tvTournamentFormat.text = "Format: $format"
                        tvTournamentDescription.text = description

                        // Setup statistics button - i dont like this button, should be like that
                        //setupViewStatisticsButton(format)
                    } else {
                        Log.e("TournamentDetails", "Tournament not found.")
                        Toast.makeText(requireContext(), "Tournament not available.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TournamentDetailsFragment", "Error fetching details: ${e.message}")
                    Toast.makeText(requireContext(), "Error loading details.", Toast.LENGTH_SHORT).show()
                }
        }
    }

//    private fun setupViewStatisticsButton(format: String) {
//        btnViewStatistics.visibility = View.VISIBLE
//        //btnViewStatistics.text = if (format == "Tables") "View Table Statistics" else "View Knockout Brackets"
//
//        btnViewStatistics.setOnClickListener {
//            when (format) {
//                "Tables" -> navigateToTableStatistics()
//                "Knockout" -> navigateToKnockoutStatistics()
//                else -> Toast.makeText(requireContext(), "Invalid format!", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    private fun navigateToTableStatistics() {
        val action = TournamentDetailsFragmentDirections
            .actionTournamentDetailsFragmentToTableStatisticsFragment(tournamentId!!)
        findNavController().navigate(action)
    }

    private fun navigateToKnockoutStatistics() {
        val action = TournamentDetailsFragmentDirections
            .actionTournamentDetailsFragmentToKnockoutStatisticsFragment(tournamentId!!)
        findNavController().navigate(action)
    }

    private fun initializeStandings(tournamentId: String) {
        db.collection("tournaments").document(tournamentId).collection("standings").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    val batch = db.batch()
                    val teamNames = listOf("Team A", "Team B", "Team C") // Replace with actual team names
                    val standingsRef = db.collection("tournaments").document(tournamentId).collection("standings")

                    teamNames.forEach { teamName ->
                        val teamDoc = standingsRef.document()
                        val teamStanding = hashMapOf(
                            "teamName" to teamName,
                            "points" to 0,
                            "wins" to 0,
                            "draws" to 0,
                            "losses" to 0,
                            "goals" to 0
                        )
                        batch.set(teamDoc, teamStanding)
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("TournamentDetails", "Standings initialized successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("TournamentDetails", "Error initializing standings: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentDetails", "Error checking standings: ${e.message}")
            }
    }

    private fun initializeKnockoutBracket(tournamentId: String) {
        db.collection("tournaments").document(tournamentId).collection("knockout_bracket").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    val batch = db.batch()
                    val matchRef = db.collection("tournaments").document(tournamentId).collection("knockout_bracket")

                    val matches = listOf(
                        Pair("Team A", "Team B"),
                        Pair("Team C", "Team D")
                    ) // Replace with actual match pairs

                    matches.forEach { (teamA, teamB) ->
                        val matchDoc = matchRef.document()
                        val match = hashMapOf(
                            "teamA" to teamA,
                            "teamB" to teamB,
                            "scoreA" to 0,
                            "scoreB" to 0,
                        )
                        batch.set(matchDoc, match)
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("TournamentDetails", "Knockout bracket initialized successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("TournamentDetails", "Error initializing knockout bracket: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentDetails", "Error checking knockout bracket: ${e.message}")
            }
    }

    private fun displayTableStandings(tournamentId: String) {
        db.collection("tournaments").document(tournamentId).collection("standings")
            .get()
            .addOnSuccessListener { snapshot ->
                val standings = snapshot.documents.flatMap { doc ->
                    (doc.get("teams") as? List<Map<String, Any>>)?.map { team ->
                        "${team["teamName"]} - ${team["points"]} points"
                    } ?: emptyList()
                }
                // Display standings in your UI
            }
            .addOnFailureListener { e ->
                Log.e("TournamentDetailsFragment", "Error fetching standings: ${e.message}")
            }
    }

    private fun displayKnockoutBrackets(tournamentId: String) {
        db.collection("tournaments").document(tournamentId).collection("matches")
            .get()
            .addOnSuccessListener { snapshot ->
                val matches = snapshot.documents.flatMap { doc ->
                    (doc.get("matches") as? List<Map<String, Any>>)?.map { match ->
                        "${match["teamA"]} vs ${match["teamB"]}"
                    } ?: emptyList()
                }
                // Display knockout brackets in your UI
            }
            .addOnFailureListener { e ->
                Log.e("TournamentDetailsFragment", "Error fetching matches: ${e.message}")
            }
    }
}