package com.example.tourniverse.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseHelper {
    private val db = FirebaseFirestore.getInstance()
    private const val TOURNAMENTS_COLLECTION = "tournaments"
    private const val USERS_COLLECTION = "users"

    /**
     * Adds a tournament to Firestore with the owner as the current user.
     *
     * @param name The name of the tournament.
     * @param teamCount The total number of teams in the tournament.
     * @param description A brief description of the tournament.
     * @param privacy Privacy level of the tournament ("Public" or "Private").
     * @param teamNames List of team names participating in the tournament.
     * @param callback Callback to indicate success (Boolean) and optional error message.
     */
    fun addTournament(
        name: String,
        teamCount: Int,
        description: String,
        privacy: String,
        teamNames: List<String>,
        callback: (Boolean, String?) -> Unit
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val ownerId = currentUser?.uid ?: return callback(false, "User not authenticated")

        val tournament = hashMapOf(
            "name" to name,
            "teamCount" to teamCount,
            "description" to description,
            "privacy" to privacy,
            "teamNames" to teamNames,
            "ownerId" to ownerId,
            "viewers" to listOf(ownerId),
            "createdAt" to System.currentTimeMillis()
        )

        val userRef = db.collection(USERS_COLLECTION).document(ownerId)

        db.collection(TOURNAMENTS_COLLECTION)
            .add(tournament)
            .addOnSuccessListener { document ->
                val tournamentId = document.id

                // Update the user's owned and viewed tournaments
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(userRef)
                    val ownedTournaments = snapshot.get("ownedTournaments") as? MutableList<String> ?: mutableListOf()
                    val viewedTournaments = snapshot.get("viewedTournaments") as? MutableList<String> ?: mutableListOf()

                    // Add tournament ID to the lists
                    ownedTournaments.add(tournamentId)
                    viewedTournaments.add(tournamentId)

                    transaction.update(userRef, "ownedTournaments", ownedTournaments)
                    transaction.update(userRef, "viewedTournaments", viewedTournaments)
                }.addOnSuccessListener {
                    callback(true, tournamentId)
                }.addOnFailureListener { e ->
                    callback(false, "Failed to update user tournaments: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                callback(false, e.message ?: "Failed to create tournament")
            }
    }

    /**
     * Fetches tournaments where the current user is either the owner or a viewer.
     *
     * @param callback Callback to return the list of tournaments as Maps.
     */
    fun getUserTournaments(callback: (List<Map<String, Any>>) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return callback(emptyList())

        val userRef = db.collection(USERS_COLLECTION).document(userId)

        userRef.get()
            .addOnSuccessListener { document ->
                val ownedTournaments = document["ownedTournaments"] as? List<String> ?: emptyList()
                val viewedTournaments = document["viewedTournaments"] as? List<String> ?: emptyList()

                val tournamentIds = ownedTournaments.union(viewedTournaments).toList()

                if (tournamentIds.isEmpty()) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                db.collection(TOURNAMENTS_COLLECTION)
                    .whereIn("id", tournamentIds)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val tournaments = querySnapshot.documents.mapNotNull { it.data }
                        callback(tournaments)
                    }
                    .addOnFailureListener {
                        callback(emptyList()) // Return empty list on failure
                    }
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    /**
     * Adds a viewer to a specific tournament document.
     *
     * @param tournamentId ID of the tournament document.
     * @param newViewerId User ID of the viewer to be added.
     * @param callback Callback to indicate success (Boolean) and optional error message.
     */
    fun addViewerToTournament(
        tournamentId: String,
        newViewerId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val tournamentRef = db.collection(TOURNAMENTS_COLLECTION).document(tournamentId)
        val userRef = db.collection(USERS_COLLECTION).document(newViewerId)

        db.runTransaction { transaction ->
            val tournamentSnapshot = transaction.get(tournamentRef)
            val viewers = tournamentSnapshot.get("viewers") as? MutableList<String> ?: mutableListOf()

            if (!viewers.contains(newViewerId)) {
                viewers.add(newViewerId)
                transaction.update(tournamentRef, "viewers", viewers)
            }

            val userSnapshot = transaction.get(userRef)
            val viewedTournaments = userSnapshot.get("viewedTournaments") as? MutableList<String> ?: mutableListOf()

            if (!viewedTournaments.contains(tournamentId)) {
                viewedTournaments.add(tournamentId)
                transaction.update(userRef, "viewedTournaments", viewedTournaments)
            }
        }.addOnSuccessListener {
            callback(true, null)
        }.addOnFailureListener { e ->
            callback(false, e.message ?: "Failed to add viewer")
        }
    }

    /**
     * Updates a field in a specific tournament document.
     *
     * @param tournamentId ID of the tournament document.
     * @param field The field name to update.
     * @param value The new value for the field.
     * @param callback Callback to indicate success (Boolean) and optional error message.
     */
    fun updateTournamentField(
        tournamentId: String,
        field: String,
        value: Any,
        callback: (Boolean, String?) -> Unit
    ) {
        db.collection(TOURNAMENTS_COLLECTION)
            .document(tournamentId)
            .update(field, value)
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
                callback(false, e.message ?: "Failed to update field")
            }
    }
}
