package com.example.tourniverse.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class TournamentSettingsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Checks the visibility of certain buttons based on the ownership and privacy settings of the tournament.
     */
    fun checkButtonVisibility(
        tournamentId: String,
        userId: String,
        onSuccess: (Boolean, Boolean, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                val ownerId = document.getString("ownerId")
                val privacy = document.getString("privacy") ?: "public"

                if (ownerId == userId) {
                    // Owner: show delete button, hide leave button, show invite button
                    onSuccess(true, false, true)
                } else {
                    // Non-owner: hide delete button, show leave button, adjust invite button visibility
                    val showInvite = privacy != "private"
                    onSuccess(false, true, showInvite)
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettingsVM", "Error checking button visibility: ${e.message}")
                onError("Failed to load tournament data.")
            }
    }

    /**
     * Loads tournament-specific notification settings while respecting global restrictions.
     */
    fun loadTournamentNotificationSettings(
        userId: String,
        tournamentId: String,
        onGlobalSettingsLoaded: (Map<String, Boolean>) -> Unit,
        onNotificationSettingsLoaded: (Map<String, Boolean>) -> Unit,
        onError: (String) -> Unit
    ) {
        loadGlobalSettings(userId,
            onSuccess = { globalSettings ->
                db.collection("users").document(userId)
                    .collection("tournaments").document(tournamentId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val notificationSettings = mapOf(
                                "Push" to (globalSettings["Push"] == true && document.getBoolean("Push") ?: true),
                                "Scores" to (globalSettings["Scores"] == true && document.getBoolean("Scores") ?: true),
                                "ChatMessages" to (globalSettings["ChatMessages"] == true && document.getBoolean("ChatMessages") ?: true),
                                "Comments" to (globalSettings["Comments"] == true && document.getBoolean("Comments") ?: true),
                                "Likes" to (globalSettings["Likes"] == true && document.getBoolean("Likes") ?: true)
                            )
                            onNotificationSettingsLoaded(notificationSettings)
                        } else {
                            Log.e("TournamentSettingsVM", "No notification settings document found.")
                            onError("Settings not available for this tournament.")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("TournamentSettingsVM", "Error loading notification settings: ${e.message}")
                        onError("Failed to load notification settings.")
                    }
            },
            onError = {
                onError(it)
            }
        )
    }

    /**
     * Loads global notification settings for the user.
     */
    private fun loadGlobalSettings(
        userId: String,
        onSuccess: (Map<String, Boolean>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users").document(userId)
            .collection("notifications").document("settings")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val globalSettings = mapOf(
                        "push" to (document.getBoolean("Push") ?: true),
                        "Scores" to (document.getBoolean("Scores") ?: true),
                        "ChatMessages" to (document.getBoolean("ChatMessages") ?: true),
                        "Comments" to (document.getBoolean("Comments") ?: true),
                        "Likes" to (document.getBoolean("Likes") ?: true)
                    )
                    Log.d("TournamentSettingsVM", "Global settings loaded: $globalSettings")
                    onSuccess(globalSettings)
                } else {
                    Log.e("TournamentSettingsVM", "Global settings document not found.")
                    onSuccess(emptyMap()) // Default to all enabled
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettingsVM", "Error loading global settings: ${e.message}")
                onError("Failed to load global settings.")
            }
    }

}
