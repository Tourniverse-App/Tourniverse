package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.MatchAdapter
import com.example.tourniverse.models.Match
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StandingsFragment : Fragment() {

    private lateinit var matchesRecyclerView: RecyclerView
    private val matches = mutableListOf<Match>()
    private lateinit var adapter: MatchAdapter
    private val db = FirebaseFirestore.getInstance()
    private val matchesCollection = db.collection("tournaments").document("tournamentId").collection("scores")
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_standings, container, false)

        matchesRecyclerView = view.findViewById(R.id.matchesRecyclerView)
        matchesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MatchAdapter(matches, ::updateMatchScore)
        matchesRecyclerView.adapter = adapter

        fetchMatches()

        return view
    }

    private fun fetchMatches() {
        matchesCollection.get()
            .addOnSuccessListener { snapshot ->
                matches.clear()
                matches.addAll(snapshot.toObjects(Match::class.java))
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load matches: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateMatchScore(matchId: String, newScoreA: Int, newScoreB: Int) {
        val ownerId = "ownerId" // Replace with actual owner ID retrieval logic

        if (currentUser?.uid != ownerId) {
            Toast.makeText(requireContext(), "Only the owner can update scores", Toast.LENGTH_SHORT).show()
            return
        }

        matchesCollection.document(matchId)
            .update(mapOf("scoreA" to newScoreA, "scoreB" to newScoreB))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Score updated successfully", Toast.LENGTH_SHORT).show()
                fetchMatches()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update score: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
