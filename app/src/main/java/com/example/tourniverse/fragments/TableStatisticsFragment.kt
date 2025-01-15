package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TableStatisticsAdapter
import com.example.tourniverse.models.TeamStanding
import com.example.tourniverse.viewmodels.TableStatisticsViewModel

class TableStatisticsFragment : Fragment() {

    private lateinit var statisticsRecyclerView: RecyclerView
    private lateinit var statisticsAdapter: TableStatisticsAdapter
    private val teamStandings = mutableListOf<TeamStanding>()
    private lateinit var viewModel: TableStatisticsViewModel
    private var tournamentId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_table_statistics, container, false)
        Log.d("TableStatisticsFragment", "onCreateView called")

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(TableStatisticsViewModel::class.java)

        tournamentId = arguments?.getString("tournamentId").orEmpty()

        if (tournamentId.isNullOrEmpty()) {
            Log.e("TableStatisticsFragment", "Tournament ID is missing.")
            Toast.makeText(context, "Tournament ID is missing.", Toast.LENGTH_SHORT).show()
            return view
        }

        initializeRecyclerView(view)
        observeTeamStandings()
        fetchTeamStandings()

        return view
    }

    private fun initializeRecyclerView(view: View) {
        statisticsRecyclerView = view.findViewById(R.id.recyclerViewTableStatistics)
        statisticsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        statisticsAdapter = TableStatisticsAdapter(teamStandings)
        statisticsRecyclerView.adapter = statisticsAdapter
    }

    private fun fetchTeamStandings() {
        tournamentId?.let {
            viewModel.fetchTeamStandings(
                it,
                onError = { errorMessage ->
                    Log.e("TableStatisticsFragment", errorMessage)
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            )
        } ?: run {
            Log.e("TableStatisticsFragment", "Tournament ID is null!")
        }
    }

    private fun observeTeamStandings() {
        viewModel.teamStandings.observe(viewLifecycleOwner) { standings ->
            updateStandings(standings)
        }
    }

    fun updateStandings(newStandings: List<TeamStanding>) {
        teamStandings.clear()
        teamStandings.addAll(newStandings)
        statisticsAdapter.notifyDataSetChanged()
        Log.d("TableStatisticsFragment", "Standings updated. Total teams: ${teamStandings.size}")
    }

    override fun onResume() {
        super.onResume()
        Log.d("TableStatisticsFragment", "onResume called - refreshing standings")
        fetchTeamStandings()
    }
}
