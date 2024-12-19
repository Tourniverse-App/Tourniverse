package com.example.tourniverse.utils

import android.util.Log
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
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
                    "ownerId" to ownerId,
                    "viewers" to emptyList<String>(),
                    "createdAt" to System.currentTimeMillis(),
                    "format" to format // Save the tournament format in Firestore
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



    fun fetchTournamentIds(callback: (List<String>?, String?) -> Unit) {
        db.collection(TOURNAMENTS_COLLECTION)
            .get()
            .addOnSuccessListener { documents ->
                val tournamentIds = documents.map { it.id }
                Log.d("FirebaseHelper", "Fetched Tournament IDs: $tournamentIds")
                callback(tournamentIds, null)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Error fetching tournaments: ${e.message}")
                callback(null, e.message ?: "Failed to fetch tournament IDs")
            }
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
        val chatRef = db.collection(TOURNAMENTS_COLLECTION).document(tournamentId).collection("chat").document()
        val welcomeMessage = hashMapOf(
            "senderId" to "System",
            "senderName" to "System",
            "message" to "Welcome to the tournament!",
            "createdAt" to System.currentTimeMillis()
        )
        batch.set(chatRef, welcomeMessage)

        // Initialize scores collection (empty for now)
        val scoresRef = db.collection(TOURNAMENTS_COLLECTION).document(tournamentId).collection("scores").document()
        val placeholderScore = hashMapOf(
            "teamA" to "",
            "teamB" to "",
            "scoreA" to 0,
            "scoreB" to 0,
            "winner" to "",
            "playedAt" to System.currentTimeMillis()
        )
        batch.set(scoresRef, placeholderScore)

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
            teamNames.forEach { teamName ->
                val doc = standingsRef.document()
                val teamStanding = hashMapOf(
                    "teamName" to teamName,
                    "points" to 0,
                    "wins" to 0,
                    "draws" to 0,
                    "losses" to 0,
                    "goals" to 0
                )
                batch.set(doc, teamStanding)
            }
        }

        // Commit the batch
        batch.commit()
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message ?: "Failed to initialize subcollections") }
    }


    fun generateMatches(
        tournamentId: String,
        teamNames: List<String>,
        format: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val matches = mutableListOf<HashMap<String, Any>>()

        if (format == "Tables") {
            // Generate round-robin matches
            for (i in teamNames.indices) {
                for (j in i + 1 until teamNames.size) {
                    matches.add(
                        hashMapOf(
                            "teamA" to teamNames[i],
                            "teamB" to teamNames[j],
                            "scoreA" to 0,
                            "scoreB" to 0,
                            "round" to 0 // No rounds in tables, but default to 0
                        )
                    )
                }
            }
        } else if (format == "Knockout") {
            // Generate initial knockout matches (first round only)
            val firstRoundMatches = teamNames.chunked(2) // Pair teams into matches
            firstRoundMatches.forEachIndexed { index, pair ->
                if (pair.size == 2) {
                    matches.add(
                        hashMapOf(
                            "teamA" to pair[0],
                            "teamB" to pair[1],
                            "scoreA" to 0,
                            "scoreB" to 0,
                            "round" to 1 // First round
                        )
                    )
                }
            }
        }

        // Save matches to Firestore
        val matchesRef = db.collection(TOURNAMENTS_COLLECTION).document(tournamentId).collection("matches")
        matchesRef.add(mapOf("matches" to matches))
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
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
     * Fetches tournaments where the current user is either the owner or a viewer.
     *
     * @param callback Callback to return the list of tournaments as Maps.
     */
    fun getUserTournaments(callback: (List<Map<String, Any>>) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return callback(emptyList())

        db.collection(USERS_COLLECTION).document(userId).get()
            .addOnSuccessListener { document ->
                val ownedTournaments = document["ownedTournaments"] as? List<String> ?: emptyList()
                val viewedTournaments = document["viewedTournaments"] as? List<String> ?: emptyList()

                val tournamentIds = ownedTournaments.union(viewedTournaments).toList()

                if (tournamentIds.isEmpty()) {
                    Log.d("FirestoreDebug", "No tournaments found for user $userId")
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                db.collection(TOURNAMENTS_COLLECTION)
                    .whereIn("__name__", tournamentIds)
                    .get()
                    .addOnSuccessListener { tournamentDocs ->
                        val tournaments = tournamentDocs.mapNotNull { it.data }
                        callback(tournaments)
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreDebug", "Error fetching tournament details: ${e.message}")
                        callback(emptyList())
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDebug", "Error fetching user document: ${e.message}")
                callback(emptyList())
            }
    }

    /**
     * Updates the standings subcollection after a match is completed.
     */
    fun updateStandings(
        tournamentId: String,
        teamA: String,
        teamB: String,
        scoreA: Int,
        scoreB: Int,
        callback: (Boolean, String?) -> Unit
    ) {
        val standingsRef = db.collection(TOURNAMENTS_COLLECTION).document(tournamentId).collection("standings")

        db.runBatch { batch ->
            // Update teamA's stats
            val teamARef = standingsRef.whereEqualTo("teamName", teamA)
            teamARef.get().addOnSuccessListener { teamADocs ->
                val teamADoc = teamADocs.documents.firstOrNull()
                teamADoc?.let {
                    val points = if (scoreA > scoreB) 3 else if (scoreA == scoreB) 1 else 0
                    val updates: Map<String, Any> = mapOf(
                        "points" to (it.getLong("points") ?: 0L) + points,
                        "goals" to (it.getLong("goals") ?: 0L) + scoreA,
                        "wins" to if (scoreA > scoreB) (it.getLong("wins") ?: 0L) + 1 else it.getLong("wins") ?: 0L,
                        "draws" to if (scoreA == scoreB) (it.getLong("draws") ?: 0L) + 1 else it.getLong("draws") ?: 0L,
                        "losses" to if (scoreA < scoreB) (it.getLong("losses") ?: 0L) + 1 else it.getLong("losses") ?: 0L
                    )
                    batch.update(teamADoc.reference, updates)
                }
            }

            // Update teamB's stats
            val teamBRef = standingsRef.whereEqualTo("teamName", teamB)
            teamBRef.get().addOnSuccessListener { teamBDocs ->
                val teamBDoc = teamBDocs.documents.firstOrNull()
                teamBDoc?.let {
                    val points = if (scoreB > scoreA) 3 else if (scoreA == scoreB) 1 else 0
                    val updates: Map<String, Any> = mapOf( // Changed to Map<String, Any>
                        "points" to (it.getLong("points") ?: 0L) + points,
                        "goals" to (it.getLong("goals") ?: 0L) + scoreB,
                        "wins" to if (scoreB > scoreA) (it.getLong("wins") ?: 0L) + 1 else it.getLong("wins") ?: 0L,
                        "draws" to if (scoreA == scoreB) (it.getLong("draws") ?: 0L) + 1 else it.getLong("draws") ?: 0L,
                        "losses" to if (scoreB < scoreA) (it.getLong("losses") ?: 0L) + 1 else it.getLong("losses") ?: 0L
                    )
                    batch.update(teamBDoc.reference, updates)
                }
            }
        }.addOnSuccessListener {
            callback(true, null)
        }.addOnFailureListener { e ->
            callback(false, e.message ?: "Failed to update standings")
        }
    }



//    fun getUserTournaments(callback: (List<Map<String, Any>>) -> Unit) {
//        val currentUser = FirebaseAuth.getInstance().currentUser
//        val userId = currentUser?.uid ?: return callback(emptyList())
//
//        val userRef = db.collection(USERS_COLLECTION).document(userId)
//
//        userRef.get()
//            .addOnSuccessListener { document ->
//                val ownedTournaments = document["ownedTournaments"] as? List<String> ?: emptyList()
//                val viewedTournaments = document["viewedTournaments"] as? List<String> ?: emptyList()
//
//                val tournamentIds = ownedTournaments.union(viewedTournaments).toList()
//
//                if (tournamentIds.isEmpty()) {
//                    Log.d("FirestoreDebug", "No tournaments found for user $userId")
//                    callback(emptyList())
//                    return@addOnSuccessListener
//                }
//
//                val tournaments = mutableListOf<Map<String, Any>>()
//                val tasks = tournamentIds.map { tournamentId ->
//                    db.collection(TOURNAMENTS_COLLECTION).document(tournamentId).get()
//                }
//
//                // Fetch all tournaments using Tasks.whenAllSuccess
//                com.google.android.gms.tasks.Tasks.whenAllSuccess<Any>(tasks)
//                    .addOnSuccessListener { results ->
//                        results.forEach { result ->
//                            val snapshot = result as? com.google.firebase.firestore.DocumentSnapshot
//                            if (snapshot != null && snapshot.exists()) {
//                                snapshot.data?.let { data ->
//                                    tournaments.add(data)
//                                    Log.d("FirestoreDebug", "Fetched tournament: ${snapshot.id} -> $data")
//                                }
//                            } else {
//                                Log.w("FirestoreDebug", "Tournament document not found: ${snapshot?.id}")
//                            }
//                        }
//                        callback(tournaments)
//                    }
//                    .addOnFailureListener { e ->
//                        Log.e("FirestoreDebug", "Error fetching tournaments: ${e.message}")
//                        callback(emptyList())
//                    }
//            }
//            .addOnFailureListener { e ->
//                Log.e("FirestoreDebug", "Error fetching user document: ${e.message}")
//                callback(emptyList())
//            }
//    }




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
            val ownerId = tournamentSnapshot.get("ownerId") as? String ?: return@runTransaction
            val viewers = tournamentSnapshot.get("viewers") as? MutableList<String> ?: mutableListOf()

            // If the user is the owner, prevent adding as viewer
            if (ownerId == newViewerId) {
                throw IllegalStateException("Owner cannot be added as a viewer")
            }

            // Update viewers list
            if (!viewers.contains(newViewerId)) {
                viewers.add(newViewerId)
                transaction.update(tournamentRef, "viewers", viewers)
            }

            // Update user's viewed tournaments
            val userSnapshot = transaction.get(userRef)
            val ownedTournaments = userSnapshot.get("ownedTournaments") as? MutableList<String> ?: mutableListOf()
            val viewedTournaments = userSnapshot.get("viewedTournaments") as? MutableList<String> ?: mutableListOf()

            // Prevent user from being a viewer if they are already the owner
            if (tournamentId in ownedTournaments) {
                throw IllegalStateException("User cannot view a tournament they own")
            }

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

    /**
     * Ensures that a user document exists in Firestore, initializing it if needed.
     *
     * @param userId The ID of the user.
     */
    fun createUserDocumentIfNotExists(userId: String) {
        val userRef = db.collection(USERS_COLLECTION).document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val userData = hashMapOf(
                    "username" to "",
                    "bio" to "",
                    "image" to null,
                    "ownedTournaments" to mutableListOf<String>(),
                    "viewedTournaments" to mutableListOf<String>()
                )
                userRef.set(userData)
                    .addOnSuccessListener {
                        println("User document created successfully")
                    }
                    .addOnFailureListener { e ->
                        println("Failed to create user document: ${e.message}")
                    }
            }
        }.addOnFailureListener { e ->
            println("Failed to check user document: ${e.message}")
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

    /**
     * Fetches tournaments where the current user is either the owner or a viewer.
     *
     * @param includeViewed If true, includes viewed tournaments. Otherwise, only owned tournaments are fetched.
     * @param callback Callback to return the list of tournaments as Maps.
     */
    fun getUserTournaments(includeViewed: Boolean, callback: (List<Map<String, Any>>) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return callback(emptyList())

        val userRef = db.collection(USERS_COLLECTION).document(userId)

        userRef.get()
            .addOnSuccessListener { document ->
                val ownedTournaments = document["ownedTournaments"] as? List<String> ?: emptyList()
                val viewedTournaments = if (includeViewed) {
                    document["viewedTournaments"] as? List<String> ?: emptyList()
                } else {
                    emptyList()
                }

                // If includeViewed is true, we just append the viewed tournaments to the owned ones.
                val tournamentIds = if (includeViewed) {
                    ownedTournaments + viewedTournaments
                } else {
                    ownedTournaments
                }

                if (tournamentIds.isEmpty()) {
                    Log.d("FirestoreDebug", "No tournaments found for user $userId")
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                val tournaments = mutableListOf<Map<String, Any>>()
                val tasks = tournamentIds.map { tournamentId ->
                    db.collection(TOURNAMENTS_COLLECTION).document(tournamentId).get()
                }

                com.google.android.gms.tasks.Tasks.whenAllSuccess<Any>(tasks)
                    .addOnSuccessListener { results ->
                        results.forEach { result ->
                            val snapshot = result as? com.google.firebase.firestore.DocumentSnapshot
                            if (snapshot != null && snapshot.exists()) {
                                snapshot.data?.let { data ->
                                    tournaments.add(data)
                                }
                            } else {
                                Log.w("FirestoreDebug", "Tournament not found or deleted")
                            }
                        }
                        callback(tournaments)
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreDebug", "Error fetching tournaments: ${e.message}")
                        callback(emptyList())
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDebug", "Error fetching user document: ${e.message}")
                callback(emptyList())
            }
    }

}
