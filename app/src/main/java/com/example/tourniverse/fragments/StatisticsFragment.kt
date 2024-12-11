package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.StatisticsAdapter
import com.example.tourniverse.models.TeamStatistics

class StatisticsFragment : Fragment() {

    private lateinit var statisticsRecyclerView: RecyclerView
    private val teamStatistics = mutableListOf<TeamStatistics>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        statisticsRecyclerView = view.findViewById(R.id.statisticsRecyclerView)
        statisticsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Mock data for team statistics
        teamStatistics.add(TeamStatistics("Team A", 2, 1, 5, 7))
        teamStatistics.add(TeamStatistics("Team B", 1, 2, 3, 3))

        val adapter = StatisticsAdapter(teamStatistics)
        statisticsRecyclerView.adapter = adapter

        // TODO: Add logic to calculate statistics from standings

        return view
    }
}
