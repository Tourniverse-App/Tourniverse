package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.StandingsAdapter
import com.example.tourniverse.models.Match
import com.example.tourniverse.models.TeamStanding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StandingsFragment : Fragment() {

    private lateinit var fixturesRecyclerView: RecyclerView
    private lateinit var fixturesAdapter: StandingsAdapter
    private lateinit var saveButton: Button

    private val db = FirebaseFirestore.getInstance()
    private var tournamentId: String? = null
    private val fixtures = mutableListOf<Match>()
    private var isOwner = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("StandingsFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_standings, container, false)

        fixturesRecyclerView = view.findViewById(R.id.recyclerViewFixtures)
        saveButton = view.findViewById(R.id.saveButton)
        fixturesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Get arguments
        tournamentId = arguments?.getString("tournamentId")

        Log.d("StandingsFragment", "tournamentId: $tournamentId")

        // Hide the button initially until ownership is verified
        saveButton.visibility = View.GONE

        // Fetch and verify ownership
        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .get()
                .addOnSuccessListener { document ->
                    val ownerId = document.getString("ownerId") ?: ""
                    val currentUserId = getCurrentUserId()

                    // Check if the current user is the owner
                    isOwner = (currentUserId == ownerId)

                    // Show save button only if the user is the owner
                    saveButton.visibility = if (isOwner) View.VISIBLE else View.GONE

                    Log.d("StandingsFragment", "User ID: $currentUserId, Owner ID: $ownerId, isOwner: $isOwner")
                }
                .addOnFailureListener { e ->
                    Log.e("StandingsFragment", "Failed to fetch owner ID: ${e.message}")
                    isOwner = false
                    saveButton.visibility = View.GONE // Hide button in case of error
                }
        }

        // Ensure standings are updated on fragment load
        updateStandings()

        // Set click listener for save button
        saveButton.setOnClickListener { saveScoresToFirestore() }

        // Initialize and set up RecyclerView adapter
        fixturesAdapter = StandingsAdapter(
            items = fixtures, // The list of matches
            updateMatchScores = { match, newScoreA, newScoreB -> updateMatchScores(match, newScoreA, newScoreB) },
            notifyScoresUpdated = { notifyScoreUpdate() } // Pass the notification callback
        )
        fixturesRecyclerView.adapter = fixturesAdapter


        fetchFixtures()

        return view
    }

    /**
     * Fetches the current user's ID using Firebase Authentication.
     */
    private fun getCurrentUserId(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser?.uid ?: "" // Returns the user's UID or an empty string if not logged in
    }


    /**
     * Fetches the fixtures (matches) from Firestore for the given tournament.
     */
    private fun fetchFixtures() {
        Log.d("StandingsFragment", "fetchFixtures called")

        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .collection("matches") // Fetch individual match documents
                .get()
                .addOnSuccessListener { documents ->
                    Log.d("StandingsFragment", "fetchFixtures success")
                    fixtures.clear()

                    for (document in documents) {
                        // Read match fields directly
                        val teamA = document.getString("teamA") ?: ""
                        val teamB = document.getString("teamB") ?: ""

                        // Handle scores safely by converting "-" to null
                        val scoreA = when (val rawScoreA = document.get("scoreA")) {
                            is String -> if (rawScoreA == "-") null else rawScoreA.toIntOrNull()
                            is Long -> rawScoreA.toInt()
                            else -> null
                        }

                        val scoreB = when (val rawScoreB = document.get("scoreB")) {
                            is String -> if (rawScoreB == "-") null else rawScoreB.toIntOrNull()
                            is Long -> rawScoreB.toInt()
                            else -> null
                        }

                        // Extract match ID
                        val matchId = document.id

                        Log.d("StandingsFragment", "Match: $teamA vs $teamB - Scores: $scoreA : $scoreB")

                        // Add match with ID to fixtures list
                        fixtures.add(Match(teamA, teamB, scoreA, scoreB, id = matchId))
                    }

                    // Notify adapter with updated data
                    fixturesAdapter.notifyDataSetChanged()
                    Log.d("StandingsFragment", "Fixtures updated in adapter")
                }
                .addOnFailureListener { e ->
                    Log.e("StandingsFragment", "fetchFixtures failed: ${e.message}")
                }
        }
    }


    /**
     * Saves the updated match scores to Firestore.
     */
    private fun saveScoresToFirestore() {
        Log.d("SaveButton", "Save button clicked.")

        tournamentId?.let { id ->
            val batch = db.batch()
            var hasInvalidScores = false

            fixtures.forEach { match ->
                val scoreA = match.scoreA
                val scoreB = match.scoreB

                Log.d("SaveButton", "Processing match: ${match.teamA} vs ${match.teamB} - Scores: $scoreA : $scoreB")

                // Check if scores are reset to initial state (-:-)
                val isResetToDefault = (scoreA == null && scoreB == null)

                // Invalid scores (e.g., one is null and the other isn't)
                if (!isResetToDefault && ((scoreA == null && scoreB != null) || (scoreB == null && scoreA != null))) {
                    Log.d("SaveButton", "Invalid scores detected for match: ${match.teamA} vs ${match.teamB}")
                    hasInvalidScores = true
                    return@forEach
                }

                if (scoreA != null && (scoreA < 0 || scoreA > 99)) {
                    Log.d("SaveButton", "Invalid score detected for team A: $scoreA")
                    hasInvalidScores = true
                    return@forEach
                }

                // Prepare data for update
                val matchData = mapOf(
                    "scoreA" to if (isResetToDefault) "-" else (scoreA ?: 0), // Reset to "-"
                    "scoreB" to if (isResetToDefault) "-" else (scoreB ?: 0)  // Reset to "-"
                )

                // Ensure match.id exists before updating
                if (match.id.isNotEmpty()) {
                    val matchRef = db.collection("tournaments").document(id)
                        .collection("matches").document(match.id) // Update the correct match
                    batch.update(matchRef, matchData)
                    Log.d("SaveButton", "Added match to batch: ${match.teamA} vs ${match.teamB}")
                } else {
                    Log.e("SaveButton", "Match ID is missing for ${match.teamA} vs ${match.teamB}")
                    hasInvalidScores = true
                }
            }

            if (hasInvalidScores) {
                Toast.makeText(
                    context,
                    "Invalid scores found. Please fix them.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            // Commit batch
            batch.commit()
                .addOnSuccessListener {
                    Log.d("SaveButton", "Scores saved successfully.")
                    Toast.makeText(context, "Scores saved successfully.", Toast.LENGTH_SHORT).show()

                    // Update standings after saving
                    updateStandings()

                    // Notify other fragments to refresh views
                    notifyStatisticsFragments()
                }
                .addOnFailureListener { e ->
                    Log.e("SaveButton", "Failed to save scores: ${e.message}")
                    Toast.makeText(context, "Failed to save scores: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
        } ?: Log.e("SaveButton", "Tournament ID is null!")
    }

    /**
     * Updates standings for Table and Knockout Statistics Fragments.
     */
    private fun notifyStatisticsFragments() {
        Log.d("StandingsFragment", "notifyStatisticsFragments called")

        // Notify Table Statistics Fragment
        val tableFragment =
            parentFragmentManager.findFragmentByTag("tableStatisticsFragment") as? TableStatisticsFragment
        if (tableFragment != null) {
            Log.d("StandingsFragment", "Updating Table Statistics Fragment")
            val tableStandings = fixtures.map { match ->
                TeamStanding(
                    teamName = match.teamA,
                    wins = 0, // Placeholder logic for wins
                    draws = 0, // Placeholder logic for draws
                    losses = 0, // Placeholder logic for losses
                    goals = match.scoreA ?: 0, // Replace null with 0 for goals
                    points = (match.scoreA ?: 0) * 3 // Example point logic
                )
            }
            tableFragment.updateStandings(tableStandings) // Updated function name
        } else {
            Log.e("StandingsFragment", "TableStatisticsFragment not found or not initialized")
        }

        // Notify Knockout Statistics Fragment
        val knockoutFragment =
            parentFragmentManager.findFragmentByTag("knockoutStatisticsFragment") as? KnockoutStatisticsFragment
        if (knockoutFragment != null) {
            Log.d("StandingsFragment", "Updating Knockout Statistics Fragment")
            val knockoutMatches = fixtures.map { match ->
                match.copy(
                    scoreA = match.scoreA ?: 0,
                    scoreB = match.scoreB ?: 0
                )
            }
            knockoutFragment.updateKnockoutMatches(knockoutMatches)
        } else {
            Log.e("StandingsFragment", "KnockoutStatisticsFragment not found or not initialized")
        }
    }

    /**
     * Updates specific match scores in Firestore.
     */
    private fun updateMatchScores(match: Match, newScoreA: Int, newScoreB: Int) {
        Log.d("StandingsFragment", "updateMatchScores called for match: ${match.teamA} vs ${match.teamB}")
        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .collection("matches")
                .whereEqualTo("teamA", match.teamA)
                .whereEqualTo("teamB", match.teamB)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val matchRef = db.collection("tournaments")
                            .document(id)
                            .collection("matches")
                            .document(document.id)

                        matchRef.update("scoreA", newScoreA, "scoreB", newScoreB)
                            .addOnSuccessListener {
                                Log.d("StandingsFragment", "Match scores updated successfully.")
                                Toast.makeText(
                                    context,
                                    "Scores updated successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                notifyStatisticsFragments()
                            }
                            .addOnFailureListener { e ->
                                Log.e("StandingsFragment", "Failed to update match scores: ${e.message}")
                            }
                    }
                }
        }
    }

    /**
     * Updates the standings for each team based on the match results.
     */
    private fun updateStandings() {
        Log.d("StandingsFragment", "updateStandings called")

        // Fetch all matches directly from Firestore
        tournamentId?.let { id ->
            db.collection("tournaments").document(id)
                .collection("matches")
                .get()
                .addOnSuccessListener { documents ->
                    Log.d("StandingsFragment", "Fetched all matches to recalculate standings")

                    val standingsMap = mutableMapOf<String, TeamStanding>()

                    for (document in documents) {
                        // Extract match data
                        val teamA = document.getString("teamA") ?: ""
                        val teamB = document.getString("teamB") ?: ""

                        // Get scores safely
                        val scoreA = when (val rawScoreA = document.get("scoreA")) {
                            is String -> if (rawScoreA == "-") null else rawScoreA.toIntOrNull()
                            is Long -> rawScoreA.toInt()
                            else -> null
                        }

                        val scoreB = when (val rawScoreB = document.get("scoreB")) {
                            is String -> if (rawScoreB == "-") null else rawScoreB.toIntOrNull()
                            is Long -> rawScoreB.toInt()
                            else -> null
                        }

                        // Skip matches with scores reset to -:- (not played)
                        if (scoreA == null && scoreB == null) {
                            Log.d("StandingsFragment", "Skipping match: $teamA vs $teamB - Scores: - : - (Not played)")
                            continue
                        }

                        // Default scores to 0 if null
                        val actualScoreA = scoreA ?: 0
                        val actualScoreB = scoreB ?: 0

                        // Get or create standings for each team
                        val standingA = standingsMap.getOrDefault(teamA, TeamStanding(teamName = teamA))
                        val standingB = standingsMap.getOrDefault(teamB, TeamStanding(teamName = teamB))

                        // Update goals
                        standingA.goals += actualScoreA
                        standingB.goals += actualScoreB

                        // Update wins, losses, and draws
                        when {
                            actualScoreA > actualScoreB -> {
                                standingA.wins += 1
                                standingA.points += 3
                                standingB.losses += 1
                            }
                            actualScoreA < actualScoreB -> {
                                standingB.wins += 1
                                standingB.points += 3
                                standingA.losses += 1
                            }
                            else -> {
                                standingA.draws += 1
                                standingA.points += 1
                                standingB.draws += 1
                                standingB.points += 1
                            }
                        }

                        // Save updated standings
                        standingsMap[teamA] = standingA
                        standingsMap[teamB] = standingB
                    }

                    // Prepare batch for Firestore updates
                    val batch = db.batch()
                    val standingsRef = db.collection("tournaments").document(id).collection("standings")

                    standingsMap.forEach { (teamName, standing) ->
                        val docRef = standingsRef.document(teamName)
                        batch.set(docRef, standing) // Update each team's standings
                        Log.d("updateStandings", "Updating standings for $teamName: $standing")
                    }

                    // Commit batch update
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("StandingsFragment", "updateStandings commit success")
                            Toast.makeText(context, "Standings updated successfully.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("StandingsFragment", "updateStandings commit failed: ${e.message}")
                            Toast.makeText(context, "Failed to update standings.", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("StandingsFragment", "Failed to fetch matches for standings: ${e.message}")
                    Toast.makeText(context, "Failed to fetch matches for standings.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // --- Notifications ---

    /**
     * Notify all users that scores have been updated, based on their notification settings.
     */
    private fun notifyScoreUpdate() {
        db.collection("tournaments").document(tournamentId!!)
            .collection("viewers").get()
            .addOnSuccessListener { viewersSnapshot ->
                viewersSnapshot.documents.forEach { document ->
                    val userId = document.id

                    db.collection("users").document(userId)
                        .collection("notifications").document("settings")
                        .get()
                        .addOnSuccessListener { settingsSnapshot ->
                            val pushEnabled = settingsSnapshot.getBoolean("Push") ?: false
                            val scoresEnabled = settingsSnapshot.getBoolean("Scores") ?: false

                            if (pushEnabled && scoresEnabled) {
                                db.collection("users").document(userId)
                                    .get()
                                    .addOnSuccessListener { userSnapshot ->
                                        val fcmToken = userSnapshot.getString("fcmToken")
                                        if (!fcmToken.isNullOrEmpty()) {
                                            sendFCMNotification(
                                                fcmToken = fcmToken,
                                                title = getTournamentName(),
                                                body = "Scores have been updated!"
                                            )
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Fragment", "Failed to fetch user FCM token: ${e.message}")
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Fragment", "Failed to fetch user notification settings: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Fragment", "Failed to fetch tournament viewers: ${e.message}")
            }
    }

    /**
     * Sends an FCM notification to the specified FCM token.
     */
    private fun sendFCMNotification(fcmToken: String, title: String, body: String) {
        val notificationData = mapOf(
            "to" to fcmToken,
            "notification" to mapOf(
                "title" to title,
                "body" to body
            ),
            "data" to mapOf(
                "type" to "Scores",
                "tournamentId" to tournamentId
            )
        )

        Log.d("FCM Notification", "Sending notification: $notificationData")

        // Replace this with your HTTP client logic (e.g., Retrofit, Volley, etc.)
    }

    /**
     * Helper to retrieve the tournament name.
     */
    private fun getTournamentName(): String {
        var tournamentName = "Tournament"
        db.collection("tournaments").document(tournamentId!!).get()
            .addOnSuccessListener { snapshot ->
                tournamentName = snapshot.getString("name") ?: "Tournament"
            }
            .addOnFailureListener { e ->
                Log.e("Fragment", "Failed to fetch tournament name: ${e.message}")
            }
        return tournamentName
    }

}
