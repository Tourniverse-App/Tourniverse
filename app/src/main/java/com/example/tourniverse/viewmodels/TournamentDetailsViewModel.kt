package com.example.tourniverse.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class TournamentDetailsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Fetch tournament details from Firestore.
     */
    fun fetchTournamentDetails(
        tournamentId: String,
        onSuccess: (String, String, String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("tournaments").document(tournamentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "Unknown Tournament"
                    val privacy = document.getString("privacy") ?: "Unknown"
                    val description = document.getString("description") ?: ""
                    val format = document.getString("format") ?: "Unknown"
                    onSuccess(name, privacy, description, format)
                } else {
                    onError("Tournament not found.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentDetailsViewModel", "Error fetching details: ${e.message}")
                onError("Error loading tournament details.")
            }
    }

    /**
     * Initialize knockout bracket if it does not exist.
     */
    fun initializeKnockoutBracket(tournamentId: String) {
        db.collection("tournaments").document(tournamentId).collection("knockout_bracket").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    val batch = db.batch()
                    val matchRef = db.collection("tournaments").document(tournamentId).collection("knockout_bracket")

                    val matches = listOf(
                        Pair("Team A", "Team B"),
                        Pair("Team C", "Team D")
                    ) // Replace with actual match pairs

                    matches.forEach { (teamA, teamB) ->
                        val matchDoc = matchRef.document()
                        val match = hashMapOf(
                            "teamA" to teamA,
                            "teamB" to teamB,
                            "scoreA" to 0,
                            "scoreB" to 0,
                        )
                        batch.set(matchDoc, match)
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("TournamentDetailsViewModel", "Knockout bracket initialized successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("TournamentDetailsViewModel", "Error initializing knockout bracket: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentDetailsViewModel", "Error checking knockout bracket: ${e.message}")
            }
    }
}
