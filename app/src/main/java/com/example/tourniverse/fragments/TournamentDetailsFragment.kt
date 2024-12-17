package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TournamentPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore

class TournamentDetailsFragment : Fragment() {

    private var tournamentId: String? = null
    private lateinit var tvTournamentName: TextView
    private lateinit var tvTournamentType: TextView
    private lateinit var tvTournamentFormat: TextView
    private lateinit var tvTournamentDescription: TextView

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get tournamentId from arguments
        arguments?.let {
            tournamentId = it.getString("tournamentId")
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

        // Fetch tournament details
        fetchTournamentDetails()

        // Setup TabLayout and ViewPager2
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val adapter = TournamentPagerAdapter(requireActivity(), tournamentId ?: "")

        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Social"
                1 -> "Standings"
                2 -> "Statistics"
                3 -> "Settings"
                else -> "Tab $position"
            }
        }.attach()

        return view
    }

    /**
     * Fetches the tournament details from Firestore using the provided tournamentId.
     * Updates the UI with tournament name, type, format, and description.
     *
     * If certain fields (like format or description) are missing, the related views are hidden.
     */
    private fun fetchTournamentDetails() {
        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        tvTournamentName.text = document.getString("name") ?: "No Name"
                        tvTournamentType.text = "Type: ${document.getString("privacy") ?: "Private"}"

                        val format = document.getString("format")
                        if (!format.isNullOrEmpty()) {
                            tvTournamentFormat.text = "Format: $format"
                            tvTournamentFormat.visibility = View.VISIBLE
                        } else {
                            tvTournamentFormat.visibility = View.GONE
                        }

                        val description = document.getString("description")
                        if (!description.isNullOrEmpty()) {
                            tvTournamentDescription.text = "Description: $description"
                            tvTournamentDescription.visibility = View.VISIBLE
                        } else {
                            tvTournamentDescription.visibility = View.GONE
                        }
                    } else {
                        Log.e("TournamentDetails", "Document not found")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TournamentDetails", "Error fetching data: ${e.message}")
                }
        } ?: run {
            Log.e("TournamentDetails", "Tournament ID is null")
        }
    }
}
