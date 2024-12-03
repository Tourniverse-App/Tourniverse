package com.example.tourniverse.utils

import com.google.firebase.firestore.FirebaseFirestore

object FirebaseHelper {
    private val db = FirebaseFirestore.getInstance()

    fun addTournament(name: String, teamCount: Int) {
        val tournament = hashMapOf(
            "name" to name,
            "teamCount" to teamCount,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("tournaments")
            .add(tournament)
            .addOnSuccessListener {
                // Successfully added
            }
            .addOnFailureListener {
                // Handle failure
            }
    }
}
