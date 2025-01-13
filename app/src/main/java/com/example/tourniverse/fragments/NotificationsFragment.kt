package com.example.tourniverse.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tourniverse.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsFragment : Fragment() {

    private lateinit var pushSwitch: Switch
    private lateinit var scoresSwitch: Switch
    private lateinit var chatSwitch: Switch
    private lateinit var commentsSwitch: Switch
    private lateinit var likesSwitch: Switch
    private lateinit var dndSwitch: Switch

    private val db = FirebaseFirestore.getInstance()
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        // Initialize switches
        pushSwitch = view.findViewById(R.id.switchPushNotifications)
        scoresSwitch = view.findViewById(R.id.switchScoresNotifications)
        chatSwitch = view.findViewById(R.id.switchChatNotifications)
        commentsSwitch = view.findViewById(R.id.switchCommentsNotifications)
        likesSwitch = view.findViewById(R.id.switchLikesNotifications)

        // Get the current user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            // Load preferences from Firebase
            loadPreferences()
        } else {
            showToast("User not authenticated")
        }

        // Set listeners for switches
        pushSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("Push", isChecked)
            showToast("Push Notifications ${if (isChecked) "Enabled" else "Disabled"}")
        }

        scoresSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("Scores", isChecked)
            showToast("Scores Notifications ${if (isChecked) "Enabled" else "Disabled"}")
        }

        chatSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("ChatMessages", isChecked)
            showToast("Chat Notifications ${if (isChecked) "Enabled" else "Disabled"}")
        }

        commentsSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("Comments", isChecked)
            showToast("Comments Notifications ${if (isChecked) "Enabled" else "Disabled"}")
        }

        likesSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("Likes", isChecked)
            showToast("Likes Notifications ${if (isChecked) "Enabled" else "Disabled"}")
        }

        return view
    }

    /**
     * Load user preferences from the Firebase Firestore subcollection.
     */
    private fun loadPreferences() {
        db.collection("users")
            .document(userId)
            .collection("notifications")
            .document("settings")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    pushSwitch.isChecked = document.getBoolean("Push") ?: true
                    scoresSwitch.isChecked = document.getBoolean("Scores") ?: true
                    chatSwitch.isChecked = document.getBoolean("ChatMessage") ?: true
                    commentsSwitch.isChecked = document.getBoolean("Comments") ?: true
                    likesSwitch.isChecked = document.getBoolean("Likes") ?: true
                }
            }
            .addOnFailureListener {
                showToast("Failed to load preferences")
            }
    }

    /**
     * Update a specific user preference in Firebase Firestore.
     */
    private fun updatePreference(key: String, value: Boolean) {
        val updates = mapOf(key to value)

        db.collection("users")
            .document(userId)
            .collection("notifications")
            .document("settings")
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                showToast("Preference Updated: $key")
            }
            .addOnFailureListener {
                showToast("Failed to update preference: $key")
            }
    }

    /**
     * Display a short toast message.
     */
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}