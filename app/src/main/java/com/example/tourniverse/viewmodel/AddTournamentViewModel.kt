package com.example.tourniverse.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.tourniverse.utils.FirebaseHelper
import com.google.firebase.firestore.FirebaseFirestore

class AddTournamentViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    fun addTournament(
        name: String,
        teamCount: Int,
        description: String,
        privacy: String,
        teamNames: List<String>,
        format: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        FirebaseHelper.addTournament(
            name = name,
            teamCount = teamCount,
            description = description,
            privacy = privacy,
            teamNames = teamNames,
            format = format
        ) { success, error ->
            onComplete(success, error)
        }
    }

    fun fetchTournamentId(
        tournamentName: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("tournaments").whereEqualTo("name", tournamentName).limit(1).get()
            .addOnSuccessListener { documents ->
                val document = documents.firstOrNull()
                if (document != null) {
                    onSuccess(document.id)
                } else {
                    onFailure("No tournament found with the name $tournamentName")
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Failed to fetch tournament ID")
            }
    }
}
