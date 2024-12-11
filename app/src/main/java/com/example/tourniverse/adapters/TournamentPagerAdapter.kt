package com.example.tourniverse.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.tourniverse.fragments.SocialFragment
import com.example.tourniverse.fragments.StandingsFragment
import com.example.tourniverse.fragments.StatisticsFragment

class TournamentPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        // Number of tabs
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        // Return the appropriate fragment for each tab
        return when (position) {
            0 -> SocialFragment()
            1 -> StandingsFragment()
            2 -> StatisticsFragment()
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}
