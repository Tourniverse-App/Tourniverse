package com.example.tourniverse.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TournamentSettingsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    fun setGameNotificationsEnabled(tournamentId: String, enabled: Boolean) {
        db.collection("users").document(userId)
            .collection("tournamentSettings").document(tournamentId)
            .update("gameNotifications", enabled)
            .addOnSuccessListener {
                // Log success
            }
            .addOnFailureListener {
                // Handle failure
            }
    }

    fun setSocialNotificationsEnabled(tournamentId: String, enabled: Boolean) {
        db.collection("users").document(userId)
            .collection("tournamentSettings").document(tournamentId)
            .update("socialNotifications", enabled)
            .addOnSuccessListener {
                // Log success
            }
            .addOnFailureListener {
                // Handle failure
            }
    }
}
