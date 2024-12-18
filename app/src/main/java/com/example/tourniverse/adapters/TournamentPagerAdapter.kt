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
    fragmentActivity: FragmentActivity,
    private val tournamentId: String
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        Log.d("TournamentPagerAdapter", "Creating fragment at position: $position with tournamentId: $tournamentId")

        return when (position) {
            0 -> SocialFragment().apply {
                arguments = Bundle().apply { putString("tournamentId", tournamentId) }
            }
            1 -> StandingsFragment().apply {
                arguments = Bundle().apply { putString("tournamentId", tournamentId) }
            }
            2 -> StatisticsFragment().apply {
                arguments = Bundle().apply { putString("tournamentId", tournamentId) }
            }
            3 -> TournamentSettingsFragment().apply {
                arguments = Bundle().apply { putString("tournamentId", tournamentId) }
            }
            else -> throw IllegalArgumentException("Invalid tab position: $position")
        }
    }

}
