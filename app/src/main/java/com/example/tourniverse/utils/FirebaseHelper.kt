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
        ownerId: String, // Add the owner's ID
        allowedViewers: List<String> = emptyList(), // Optional viewer IDs
        callback: (Boolean, String?) -> Unit
    ) {
        val tournament = hashMapOf(
            "name" to name,
            "teamCount" to teamCount,
            "description" to description,
            "privacy" to privacy,
            "teamNames" to teamNames,
            "ownerId" to ownerId, // Add the authenticated user ID
            "allowedViewers" to allowedViewers, // Empty by default
            "shareableLink" to "https://yourapp/tournament/${System.currentTimeMillis()}",
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("tournaments")
            .add(tournament)
            .addOnSuccessListener { documentReference ->
                callback(true, documentReference.id)
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

}
