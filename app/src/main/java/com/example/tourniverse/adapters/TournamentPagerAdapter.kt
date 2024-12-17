package com.example.tourniverse.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tournamentapp.ui.settings.TournamentSettingsFragment
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
        return when (position) {
            0 -> SocialFragment()
            1 -> StandingsFragment()
            2 -> StatisticsFragment()
            3 -> TournamentSettingsFragment().apply {
                arguments = Bundle().apply {
                    putString("tournamentId", tournamentId)
                }
            }
            else -> throw IllegalArgumentException("Invalid tab position: $position")
        }
    }
}
