package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TournamentAdapter
import com.example.tourniverse.models.Tournament
import com.example.tourniverse.viewmodels.HomeViewModel

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noTournamentsView: TextView
    private lateinit var adapter: TournamentAdapter
    private lateinit var viewModel: HomeViewModel
    private val tournaments = mutableListOf<Tournament>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        Log.d("HomeFragment", "onCreateView called")

        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        recyclerView = view.findViewById(R.id.recyclerTournaments)
        noTournamentsView = view.findViewById(R.id.noTournamentsView)
        val searchBar = view.findViewById<EditText>(R.id.searchBar)

        recyclerView.layoutManager = GridLayoutManager(context, 2)
        adapter = TournamentAdapter(tournaments, ::navigateToTournamentDetails)
        recyclerView.adapter = adapter

        searchBar.addTextChangedListener(adapter.getSearchTextWatcher())

        fetchUserTournaments() // Fetch tournaments when the view is created
        observeTournaments()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val joinTournamentButton = view.findViewById<Button>(R.id.buttonJoinTournament)
        joinTournamentButton.setOnClickListener { showJoinTournamentDialog() }
    }

    private fun fetchUserTournaments() {
        viewModel.fetchUserTournaments(
            onResult = { fetchedTournaments ->
                tournaments.clear()
                tournaments.addAll(fetchedTournaments)
                adapter.filter("")
                adapter.notifyDataSetChanged()

                if (tournaments.isEmpty()) {
                    showNoTournamentsMessage()
                } else {
                    recyclerView.visibility = View.VISIBLE
                    noTournamentsView.visibility = View.GONE
                }
            },
            onError = { errorMessage ->
                showNoTournamentsMessage()
                Log.e("HomeFragment", errorMessage)
            }
        )
    }

    private fun showNoTournamentsMessage() {
        recyclerView.visibility = View.GONE
        noTournamentsView.visibility = View.VISIBLE
        Toast.makeText(requireContext(), "No tournaments found.", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToTournamentDetails(tournament: Tournament) {
        Log.d("HomeFragment", "Navigating to tournament details for ID: ${tournament.id}")
        Toast.makeText(requireContext(), "Opening tournament: ${tournament.name}", Toast.LENGTH_SHORT).show()

        val bundle = Bundle().apply {
            putString("tournamentId", tournament.id)
            putString("tournamentName", tournament.name)
            putString("tournamentType", tournament.type)
            putString("tournamentFormat", tournament.format)
            putString("tournamentDescription", tournament.description)
        }
        findNavController().navigate(R.id.action_homeFragment_to_tournamentDetailsFragment, bundle)
    }

    private fun showJoinTournamentDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_join_tournament, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Join Tournament")
            .setView(dialogView)
            .setPositiveButton("Join") { _, _ ->
                val codeInput = dialogView.findViewById<EditText>(R.id.tournamentCodeInput)
                val tournamentCode = codeInput.text.toString().trim()

                if (tournamentCode.isNotEmpty()) {
                    joinTournament(tournamentCode)
                } else {
                    Toast.makeText(context, "Please enter a valid tournament code.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun joinTournament(tournamentId: String) {
        viewModel.joinTournament(
            tournamentId,
            onSuccess = { refreshFragment() },
            onError = { showToast(it) }
        )
    }

    private fun refreshFragment() {
        findNavController().run {
            popBackStack()
            navigate(R.id.nav_home)
        }
    }

    private fun observeTournaments() {
        viewModel.tournaments.observe(viewLifecycleOwner) { updatedTournaments ->
            tournaments.clear()
            tournaments.addAll(updatedTournaments)
            adapter.notifyDataSetChanged()

            if (tournaments.isEmpty()) {
                showNoTournamentsMessage()
            } else {
                recyclerView.visibility = View.VISIBLE
                noTournamentsView.visibility = View.GONE
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
