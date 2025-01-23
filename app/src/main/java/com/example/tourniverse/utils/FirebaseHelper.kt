package com.example.tourniverse.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tourniverse.activities.LoginActivity
import com.example.tourniverse.activities.MainActivity
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
     * @param callback Callback to indicate success (Boolean) and optional error message.
     */
    fun addTournament(
        name: String,
        teamCount: Int,
        description: String,
        privacy: String,
        teamNames: List<String>,
        format: String, // Added parameter to handle tournament format (Tables)
        callback: (Boolean, String?) -> Unit
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val ownerId = currentUser?.uid ?: return callback(false, "User not authenticated")


        // Fetch all tournaments to log IDs or perform validation
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
     * @param format The format of the tournament (Tables).
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
            "profilePhoto" to "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",
                    "message" to "Welcome to the tournament, please respect everyone and be nice!",
            "createdAt" to System.currentTimeMillis()
        )
        batch.set(chatRef, welcomeMessage)

        // Generate matches based on format
        generateMatches(tournamentId, teamNames, format) { success, error ->
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

    /**
     * Updates the user's ownedTournaments list.
     */
    private fun updateUserOwnedTournaments(
        userId: String,
        tournamentId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val userRef = db.collection(USERS_COLLECTION).document(userId)
        val tournamentData = hashMapOf(
            "isOwner" to true,
            "Push" to false,
            "ChatMessages" to false,
            "Comments" to false,
            "Likes" to false,
            "Dnd" to false
        )

        userRef.collection("tournaments").document(tournamentId).set(tournamentData)
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
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

    fun deleteTournament(context: Context, tournamentId: String, isAccountDeletion: Boolean) {
        if (!isAccountDeletion) {
            // Redirect to home before deleting
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("REFRESH_HOME", true)
            context.startActivity(intent)
        }

        // Fetch all viewers and owner ID
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                val viewers = document.get("viewers") as? List<String> ?: emptyList()
                val ownerId = document.getString("ownerId") ?: ""

                // Remove the tournament ID from all viewers' tournaments subcollection
                for (viewerId in viewers) {
                    db.collection("users").document(viewerId)
                        .collection("tournaments").document(tournamentId).delete()
                        .addOnSuccessListener {
                            Log.d("FirebaseHelper", "Removed tournament from viewer: $viewerId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseHelper", "Failed to remove tournament from viewer $viewerId: ${e.message}")
                        }
                }

                // Remove the tournament ID from the owner's tournaments subcollection
                db.collection("users").document(ownerId)
                    .collection("tournaments").document(tournamentId).delete()
                    .addOnSuccessListener {
                        Log.d("FirebaseHelper", "Removed tournament from owner's tournaments.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseHelper", "Failed to remove tournament from owner's tournaments: ${e.message}")
                    }

                // Delete subcollections first, then the main document
                deleteSubcollectionsAndDocument(tournamentId)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Failed to fetch tournament details: ${e.message}")
            }
    }

    fun deleteSubcollectionsAndDocument(documentId: String) {
        val documentRef = db.collection("tournaments").document(documentId)

        documentRef.collection("chat").get().addOnSuccessListener { chatSnapshots ->
            for (doc in chatSnapshots) {
                doc.reference.delete()
            }

            documentRef.collection("matches").get().addOnSuccessListener { matchSnapshots ->
                for (doc in matchSnapshots) {
                    doc.reference.delete()
                }

                documentRef.collection("standings").get().addOnSuccessListener { standingsSnapshots ->
                    for (doc in standingsSnapshots) {
                        doc.reference.delete()
                    }

                    // Finally, delete the main document after subcollections are cleared
                    documentRef.delete()
                        .addOnSuccessListener {
                            Log.d("FirebaseHelper", "Tournament and subcollections deleted successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseHelper", "Failed to delete tournament: ${e.message}")
                        }
                }
            }
        }
    }

    fun deleteUserData(userId: String, onComplete: (() -> Unit)? = null) {
        val userRef = db.collection("users").document(userId)

        userRef.collection("notifications").get().addOnSuccessListener { notificationSnapshots ->
            for (doc in notificationSnapshots) {
                doc.reference.delete()
            }

            userRef.collection("tournaments").get().addOnSuccessListener { tournamentSnapshots ->
                for (doc in tournamentSnapshots) {
                    doc.reference.delete()
                }

                // Finally, delete the main document
                userRef.delete()
                    .addOnSuccessListener {
                        Log.d("FirebaseHelper", "User document and subcollections deleted successfully.")
                        onComplete?.invoke()
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseHelper", "Failed to delete user document: ${e.message}")
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("FirebaseHelper", "Failed to delete user subcollections: ${e.message}")
        }
    }

}
