package com.example.tourniverse.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.tourniverse.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.tourniverse.adapters.TournamentPagerAdapter

class TournamentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        // Retrieve tournamentId passed through intent
        val tournamentId = intent.getStringExtra("tournamentId") ?: ""
        val tournamentFormat = intent.getStringExtra("tournamentFormat") ?: "Tables"

        // Pass the tournamentId and tournamentFormat to the adapter
        val adapter = TournamentPagerAdapter(this, tournamentId, tournamentFormat)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Social"
                1 -> "Standings"
                2 -> "Statistics"
                3 -> "Settings"
                else -> ""
            }
        }.attach()
    }
}