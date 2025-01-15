package com.example.tourniverse.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tourniverse.models.TeamStanding
import com.google.firebase.firestore.FirebaseFirestore

class TableStatisticsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _teamStandings = MutableLiveData<List<TeamStanding>>()
    val teamStandings: LiveData<List<TeamStanding>> get() = _teamStandings

    /**
     * Fetches team standings for a given tournament ID from Firestore.
     */
    fun fetchTeamStandings(tournamentId: String, onError: (String) -> Unit) {
        Log.d("TableStatisticsViewModel", "Fetching standings for tournament ID: $tournamentId")

        db.collection("tournaments").document(tournamentId)
            .collection("standings")
            .get()
            .addOnSuccessListener { documents ->
                val standingsList = mutableListOf<TeamStanding>()

                for (document in documents) {
                    val teamName = document.id
                    val wins = document.getLong("wins")?.toInt() ?: 0
                    val draws = document.getLong("draws")?.toInt() ?: 0
                    val losses = document.getLong("losses")?.toInt() ?: 0
                    val goals = document.getLong("goals")?.toInt() ?: 0
                    val points = document.getLong("points")?.toInt() ?: 0

                    standingsList.add(
                        TeamStanding(
                            teamName = teamName,
                            wins = wins,
                            draws = draws,
                            losses = losses,
                            goals = goals,
                            points = points
                        )
                    )
                }

                standingsList.sortWith(
                    compareByDescending<TeamStanding> { it.points }
                        .thenByDescending { it.goals }
                        .thenBy { it.teamName }
                )

                _teamStandings.postValue(standingsList)
            }
            .addOnFailureListener { e ->
                Log.e("TableStatisticsViewModel", "Failed to fetch standings: ${e.message}")
                onError("Failed to load standings.")
            }
    }
}
