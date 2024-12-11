package com.example.tourniverse.fragments

import android.os.Bundle
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

class TournamentDetailsFragment : Fragment() {

    private lateinit var tournamentName: String
    private lateinit var tournamentType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tournamentName = it.getString("tournamentName") ?: "Unknown Tournament"
            tournamentType = it.getString("tournamentType") ?: "Unknown Type"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tournament_details, container, false)

        // Set up the toolbar with the tournament name
        val tvTournamentName = view.findViewById<TextView>(R.id.tvTournamentName)
        tvTournamentName.text = tournamentName

        // Set up TabLayout and ViewPager2
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val adapter = TournamentPagerAdapter(requireActivity())

        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Social"
                1 -> "Standings"
                2 -> "Statistics"
                else -> "Tab $position"
            }
        }.attach()

        return view
    }
}
