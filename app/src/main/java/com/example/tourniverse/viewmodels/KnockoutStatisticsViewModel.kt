package com.example.tourniverse.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.tourniverse.models.Match
import com.google.firebase.firestore.FirebaseFirestore

class KnockoutStatisticsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    val knockoutMatches = mutableListOf<Match>()

    fun fetchKnockoutMatches(
        tournamentId: String,
        onSuccess: (List<Match>) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d("KnockoutStatisticsViewModel", "fetchKnockoutMatches called")
        db.collection("tournaments").document(tournamentId).collection("matches")
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("KnockoutStatisticsViewModel", "fetchKnockoutMatches success")
                knockoutMatches.clear()
                for (document in snapshot.documents) {
                    val matchesArray = document.get("matches") as? List<Map<String, Any>> ?: continue
                    matchesArray.forEach { match ->
                        val teamA = match["teamA"] as? String ?: ""
                        val teamB = match["teamB"] as? String ?: ""
                        val scoreA = (match["scoreA"] as? Long)?.toInt() ?: 0
                        val scoreB = (match["scoreB"] as? Long)?.toInt() ?: 0

                        // Only add matches that have scores updated
                        if (!(scoreA == 0 && scoreB == 0)) {
                            knockoutMatches.add(Match(teamA, teamB, scoreA, scoreB))
                        }
                    }
                }
                onSuccess(knockoutMatches)
            }
            .addOnFailureListener { e ->
                Log.e("KnockoutStatisticsViewModel", "Failed to fetch knockout matches: ${e.message}")
                onError("Failed to load matches.")
            }
    }
}
