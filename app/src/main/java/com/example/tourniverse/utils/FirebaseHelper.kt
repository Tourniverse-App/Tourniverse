package com.example.tourniverse.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseHelper {
    private val db = FirebaseFirestore.getInstance()
    private const val TOURNAMENTS_COLLECTION = "tournaments"

    /**
     * Adds a tournament to Firestore.
     *
     * @param name The name of the tournament.
     * @param teamCount The number of teams in the tournament.
     * @param description The tournament description.
     * @param privacy The privacy setting of the tournament ("Public" or "Private").
     * @param teamNames A list of team names participating in the tournament.
     * @param callback A lambda function to handle success and failure with the document ID.
     */
    fun addTournament(
        name: String,
        teamCount: Int,
        description: String,
        privacy: String,
        teamNames: List<String>,
        callback: (Boolean, String?) -> Unit
    ) {
        val tournament = mapOf(
            "name" to name,
            "teamCount" to teamCount,
            "description" to description,
            "privacy" to privacy,
            "teamNames" to teamNames,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection(TOURNAMENTS_COLLECTION)
            .add(tournament)
            .addOnSuccessListener { documentReference ->
                val documentId = documentReference.id
                Log.d("FirebaseHelper", "Tournament added with ID: $documentId")
                callback(true, documentId) // Return the document ID on success
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Error adding tournament: ${e.message}", e)
                callback(false, e.message) // Return error message on failure
            }
    }
}
