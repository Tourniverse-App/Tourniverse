package com.example.tourniverse.adapters

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tourniverse.fragments.*

/**
 * Adapter for managing fragments in TournamentDetailsFragment's ViewPager2.
 * Supports Social, Standings, Statistics, and Settings tabs.
 *
 * @param fragmentActivity The activity hosting the ViewPager2.
 * @param tournamentId The ID of the current tournament, passed to relevant fragments.
 */
class TournamentPagerAdapter(
    private val fragmentActivity: FragmentActivity,
    private val tournamentId: String,
    private val tournamentFormat: String
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        Log.d("TournamentPagerAdapter", "Creating fragment at position: $position with tournamentId: $tournamentId")

        // Centralize fragment creation logic for consistency
        val fragment = when (position) {
            0 -> SocialFragment()
            1 -> StandingsFragment()
            2 -> if (tournamentFormat == "Tables") TableStatisticsFragment() else KnockoutStatisticsFragment()
            3 -> TournamentSettingsFragment()
            else -> throw IllegalArgumentException("Invalid tab position: $position")
        }

        // Attach tournamentId to the fragment
        fragment.arguments = Bundle().apply {
            putString("tournamentId", tournamentId)
            putString("tournamentFormat", fragmentActivity.intent.getStringExtra("tournamentFormat"))
        }
        return fragment
    }
}