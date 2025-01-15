package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
 */
class TournamentDetailsFragment : Fragment() {

    private var tournamentId: String? = null
    private lateinit var tvTournamentName: TextView
    private lateinit var tvTournamentType: TextView
    private lateinit var tvTournamentFormat: TextView
    private lateinit var tvTournamentDescription: TextView

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve tournamentId from arguments
        tournamentId = arguments?.getString("tournamentId")
        if (tournamentId.isNullOrEmpty()) {
            Log.e("TournamentDetails", "Tournament ID is null or empty.")
            Toast.makeText(requireContext(), "Invalid tournament ID.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_tournament_details, container, false)

        // Initialize views
        tvTournamentName = view.findViewById(R.id.tvTournamentName)
        tvTournamentType = view.findViewById(R.id.tvTournamentType)
        tvTournamentFormat = view.findViewById(R.id.tvTournamentFormat)
        tvTournamentDescription = view.findViewById(R.id.tvTournamentDescription)

        // Fetch tournament details
        if (!tournamentId.isNullOrEmpty()) {
            fetchTournamentDetails()
        }

        // Setup TabLayout and ViewPager2
        setupViewPager(view)

        return view
    }

    private fun setupViewPager(view: View) {
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)

        val adapter = TournamentPagerAdapter(requireActivity(), tournamentId ?: "", "Tables") // Example format
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

                        // Update UI
                        tvTournamentName.text = name
                        tvTournamentType.text = "Type: $privacy"
                        tvTournamentFormat.text = "Format: $format"
                        tvTournamentDescription.text = description

                        // Automatically initialize knockout bracket if needed
                        if (format == "Knockout") {
                            initializeKnockoutBracket(id)
                        }
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
