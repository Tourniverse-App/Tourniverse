package com.example.tourniverse.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Load user preferences from the Firebase Firestore subcollection.
     */
    fun loadPreferences(
        userId: String,
        onSuccess: (Map<String, Boolean>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users")
            .document(userId)
            .collection("notifications")
            .document("settings")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val preferences = mapOf(
                        "Push" to (document.getBoolean("Push") ?: true),
                        "Scores" to (document.getBoolean("Scores") ?: true),
                        "ChatMessages" to (document.getBoolean("ChatMessages") ?: true),
                        "Comments" to (document.getBoolean("Comments") ?: true),
                        "Likes" to (document.getBoolean("Likes") ?: true)
                    )
                    onSuccess(preferences)
                } else {
                    onError("Preferences document does not exist")
                }
            }
            .addOnFailureListener { e ->
                Log.e("NotificationsViewModel", "Failed to load preferences: ${e.message}")
                onError("Failed to load preferences")
            }
    }
}
