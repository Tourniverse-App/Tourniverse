package com.example.tourniverse.utils

import android.util.Log
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.tourniverse.models.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
     * @param imageResId Resource ID for the selected image.
     * @param callback Callback to indicate success (Boolean) and optional error message.
     */
    fun addTournament(
        name: String,
        teamCount: Int,
        description: String,
        privacy: String,
        teamNames: List<String>,
        format: String, // Added parameter to handle tournament format (Knockout or Tables)
        callback: (Boolean, String?) -> Unit
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val ownerId = currentUser?.uid ?: return callback(false, "User not authenticated")

        // Fetch all tournaments to log IDs or perform validation (optional)
        db.collection(TOURNAMENTS_COLLECTION)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d("FirebaseHelper", "Existing Tournament ID: ${document.id}")
                }

                val tournamentData = hashMapOf(
                    "name" to name,
                    "teamCount" to teamCount,
                    "description" to description,
                    "privacy" to privacy,
                    "teamNames" to teamNames,
                    "format" to format, // Save the tournament format in Firestore
                    "ownerId" to ownerId,
                    "viewers" to emptyList<String>(),
                    "memberCount" to 1,
                    "createdAt" to System.currentTimeMillis()
                )

                val tournamentRef = db.collection(TOURNAMENTS_COLLECTION)

                tournamentRef.add(tournamentData)
                    .addOnSuccessListener { documentRef ->
                        val tournamentId = documentRef.id
                        Log.d("FirestoreDebug", "Tournament created with ID: $tournamentId")

                        // Initialize subcollections
                        initializeSubcollections(tournamentId, teamNames, format) { success, error ->
                            if (success) {
                                updateUserOwnedTournaments(ownerId, tournamentId) { userUpdateSuccess, userError ->
                                    if (userUpdateSuccess) {
                                        Log.d("FirestoreDebug", "Successfully updated user's ownedTournaments.")
                                        callback(true, null)
                                    } else {
                                        Log.e("FirestoreDebug", "Error updating user: $userError")
                                        callback(false, "Failed to update user: $userError")
                                    }
                                }
                            } else {
                                Log.e("FirestoreDebug", "Error initializing subcollections: $error")
                                callback(false, "Failed to initialize subcollections: $error")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreDebug", "Error adding tournament: ${e.message}")
                        callback(false, e.message ?: "Failed to create tournament")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Error fetching tournaments: ${e.message}")
                callback(false, e.message)
            }
    }

    /**
     * Adds a comment to a tournament's chat collection.
     *
     * @param tournamentId ID of the tournament.
     */
    fun incrementMemberCount(tournamentId: String) {
        db.collection("tournaments").document(tournamentId)
            .update("memberCount", FieldValue.increment(1))
            .addOnSuccessListener { Log.d("FirebaseHelper", "Member count incremented.") }
            .addOnFailureListener { e -> Log.e("FirebaseHelper", "Failed to increment member count: ${e.message}") }
    }

    /**
     * Adds a comment to a tournament's chat collection.
     *
     * @param tournamentId ID of the tournament.
     */
    fun decrementMemberCount(tournamentId: String) {
        db.collection("tournaments").document(tournamentId)
            .update("memberCount", FieldValue.increment(-1))
            .addOnSuccessListener { Log.d("FirebaseHelper", "Member count decremented.") }
            .addOnFailureListener { e -> Log.e("FirebaseHelper", "Failed to decrement member count: ${e.message}") }
    }

    /**
     * Initializes subcollections for a new tournament.
     *
     * @param tournamentId ID of the new tournament.
     * @param teamNames List of team names participating in the tournament.
     * @param format The format of the tournament (Knockout or Tables).
     * @param callback Callback to indicate success (Boolean) and optional error message.
     */
    private fun initializeSubcollections(
        tournamentId: String,
        teamNames: List<String>,
        format: String, // Added to handle format type
        callback: (Boolean, String?) -> Unit
    ) {
        val batch = db.batch()

        // Initialize chat collection with a welcome message
        val chatRef = db.collection(TOURNAMENTS_COLLECTION).document(tournamentId)
            .collection("chat").document()
        val welcomeMessage = hashMapOf(
            "senderId" to "System",
            "senderName" to "System",
            "message" to "Welcome to the tournament, please respect everyone and be nice!",
            "createdAt" to System.currentTimeMillis()
        )
        batch.set(chatRef, welcomeMessage)

        // Generate matches based on format
        FirebaseHelper.generateMatches(tournamentId, teamNames, format) { success, error ->
            if (success) {
                Log.d("FirebaseHelper", "Matches initialized successfully for format: $format")
            } else {
                Log.e("FirebaseHelper", "Error initializing matches: $error")
                callback(false, error ?: "Error initializing matches")
                return@generateMatches
            }
        }

        // Initialize standings for Tables format
        if (format == "Tables") {
            val standingsRef = db.collection(TOURNAMENTS_COLLECTION).document(tournamentId).collection("standings")

            // Use team names as document IDs instead of generating random IDs
            teamNames.forEach { teamName ->
                val teamStanding = hashMapOf(
                    "teamName" to teamName,
                    "points" to 0,
                    "wins" to 0,
                    "draws" to 0,
                    "losses" to 0,
                    "goals" to 0
                )
                batch.set(standingsRef.document(teamName), teamStanding) // Document ID = Team Name
            }
        }

        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                Log.d("initializeSubcollections", "Subcollections initialized successfully.")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("initializeSubcollections", "Failed to initialize subcollections: ${e.message}")
                callback(false, e.message ?: "Failed to initialize subcollections")
            }
    }

    fun generateMatches(
        tournamentId: String,
        teamNames: List<String>,
        format: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val batch = db.batch() // Use a batch for efficiency and consistency

        if (format == "Tables") {
            // Generate round-robin matches
            for (i in teamNames.indices) {
                for (j in i + 1 until teamNames.size) {
                    // Create match data with unique ID
                    val matchId = "${teamNames[i]}_${teamNames[j]}"
                    val matchData = hashMapOf(
                        "id" to matchId,                  // Unique ID
                        "teamA" to teamNames[i],
                        "teamB" to teamNames[j],
                        "scoreA" to null,                 // Null for initial scores
                        "scoreB" to null
                    )
                    Log.d("generateMatches", "Adding match: $matchData")

                    // Prepare to save each match as a separate document
                    val matchRef = db.collection(TOURNAMENTS_COLLECTION)
                        .document(tournamentId)
                        .collection("matches")
                        .document(matchId) // Use unique ID as Firestore document ID

                    batch.set(matchRef, matchData)
                }
            }
        } else if (format == "Knockout") {
            // Generate initial knockout matches (first round only)
            val firstRoundMatches = teamNames.chunked(2) // Pair teams into matches
            firstRoundMatches.forEachIndexed { index, pair ->
                if (pair.size == 2) {
                    // Create match data with unique ID
                    val matchId = "${pair[0]}_${pair[1]}"
                    val matchData = hashMapOf(
                        "id" to matchId,                  // Unique ID
                        "teamA" to pair[0],
                        "teamB" to pair[1],
                        "scoreA" to null,                 // Null for initial scores
                        "scoreB" to null,
                        "round" to 1                      // First round
                    )
                    Log.d("generateMatches", "Adding knockout match: $matchData")

                    // Prepare to save each match as a separate document
                    val matchRef = db.collection(TOURNAMENTS_COLLECTION)
                        .document(tournamentId)
                        .collection("matches")
                        .document(matchId) // Use unique ID as Firestore document ID

                    batch.set(matchRef, matchData)
                }
            }
        }

        // Commit batch updates to Firestore
        batch.commit()
            .addOnSuccessListener {
                Log.d("generateMatches", "Matches successfully generated!")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("generateMatches", "Failed to generate matches: ${e.message}")
                callback(false, e.message ?: "Failed to generate matches")
            }
    }

    fun progressKnockoutRound(
        tournamentId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val matchesRef = db.collection(TOURNAMENTS_COLLECTION).document(tournamentId).collection("matches")

        // Fetch all matches in the current round
        matchesRef.whereEqualTo("round", getCurrentRound(tournamentId)).get()
            .addOnSuccessListener { snapshot ->
                val nextRoundMatches = mutableListOf<HashMap<String, Any>>()
                val winners = mutableListOf<String>()

                snapshot.documents.forEach { doc ->
                    val match = doc.data
                    val teamA = match?.get("teamA") as? String
                    val teamB = match?.get("teamB") as? String
                    val scoreA = (match?.get("scoreA") as? Long)?.toInt() ?: 0
                    val scoreB = (match?.get("scoreB") as? Long)?.toInt() ?: 0

                    // Determine the winner
                    if (scoreA > scoreB) {
                        teamA?.let { winners.add(it) }
                    } else if (scoreB > scoreA) {
                        teamB?.let { winners.add(it) }
                    }
                }

                // Pair winners for the next round
                winners.chunked(2).forEach { pair ->
                    if (pair.size == 2) {
                        nextRoundMatches.add(
                            hashMapOf(
                                "teamA" to pair[0],
                                "teamB" to pair[1],
                                "scoreA" to 0,
                                "scoreB" to 0,
                                "round" to getCurrentRound(tournamentId) + 1 // Increment round
                            )
                        )
                    }
                }

                // Save next round matches to Firestore
                matchesRef.add(mapOf("matches" to nextRoundMatches))
                    .addOnSuccessListener {
                        callback(true, null)
                    }
                    .addOnFailureListener { e ->
                        callback(false, e.message ?: "Failed to progress knockout round")
                    }
            }
            .addOnFailureListener { e ->
                callback(false, e.message ?: "Failed to fetch matches")
            }
    }

    // Helper function to get the current round for the tournament
    private fun getCurrentRound(tournamentId: String): Int {
        // Fetch matches and determine the highest round
        // This can be optimized with a Firestore field or a more efficient query
        return 1 // Placeholder logic
    }

    /**
     * Updates the user's ownedTournaments list.
     */
    private fun updateUserOwnedTournaments(
        userId: String,
        tournamentId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val userRef = db.collection(USERS_COLLECTION).document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val ownedTournaments = snapshot.get("ownedTournaments") as? MutableList<String> ?: mutableListOf()
            if (!ownedTournaments.contains(tournamentId)) {
                ownedTournaments.add(tournamentId)
                transaction.update(userRef, "ownedTournaments", ownedTournaments)
            }
        }.addOnSuccessListener {
            callback(true, null)
        }.addOnFailureListener { e ->
            callback(false, e.message ?: "Failed to update user owned tournaments")
        }
    }

    /**
     * Fetches the user document for a specific user ID.
     *
     * @param userId The ID of the user.
     * @param callback Callback to return the user document as a Map.
     */
    fun getUserDocument(userId: String, callback: (Map<String, Any>?) -> Unit) {
        val userRef = db.collection(USERS_COLLECTION).document(userId)
        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    callback(document.data)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                callback(null)
            }
    }

    fun updatePostLikes(postId: String, likesCount: Int, likedBy: List<String>, tournamentId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("tournaments")
            .document(tournamentId)
            .collection("chat")
            .document(postId)
            .update(
                mapOf(
                    "likesCount" to likesCount,
                    "likedBy" to likedBy
                )
            )
            .addOnSuccessListener {
                Log.d("FirebaseHelper", "Likes updated successfully!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Error updating likes: ${e.message}")
            }
    }
}
