package com.example.tourniverse.fragments

import android.content.Context
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
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsFragment : Fragment() {

    private lateinit var pushSwitch: Switch
    private lateinit var scoresSwitch: Switch
    private lateinit var chatSwitch: Switch
    private lateinit var commentsSwitch: Switch
    private lateinit var likesSwitch: Switch
    private lateinit var dndSwitch: Switch

    private val db = FirebaseFirestore.getInstance()
    private val userId = "exampleUserId" // Replace with actual user ID from authentication

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
        dndSwitch = view.findViewById(R.id.switchDndMode)

        // Load preferences from Firebase
        loadPreferences()

        // Set listeners for switches
        pushSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("push_notifications", isChecked)
            showToast("Push Notifications ${if (isChecked) "Enabled" else "Disabled"}")
        }

        scoresSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("scores_notifications", isChecked)
            showToast("Scores Notifications ${if (isChecked) "Enabled" else "Disabled"}")
        }

        chatSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("chat_notifications", isChecked)
            showToast("Chat Notifications ${if (isChecked) "Enabled" else "Disabled"}")
        }

        commentsSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("comments_notifications", isChecked)
            showToast("Comments Notifications ${if (isChecked) "Enabled" else "Disabled"}")
        }

        likesSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("likes_notifications", isChecked)
            showToast("Likes Notifications ${if (isChecked) "Enabled" else "Disabled"}")
        }

        dndSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("dnd_mode", isChecked)
            if (isChecked) {
                showToast("Do Not Disturb Mode Enabled for 24 hours")
                deactivateDndAfter24Hours()
            } else {
                showToast("Do Not Disturb Mode Disabled")
            }
        }

        return view
    }

    /**
     * Load user preferences from the Firebase Firestore subcollection.
     */
    private fun loadPreferences() {
        db.collection("users")
            .document(userId)
            .collection("preferences")
            .document("notifications")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    pushSwitch.isChecked = document.getBoolean("push_notifications") ?: true
                    scoresSwitch.isChecked = document.getBoolean("scores_notifications") ?: true
                    chatSwitch.isChecked = document.getBoolean("chat_notifications") ?: true
                    commentsSwitch.isChecked = document.getBoolean("comments_notifications") ?: true
                    likesSwitch.isChecked = document.getBoolean("likes_notifications") ?: true
                    dndSwitch.isChecked = document.getBoolean("dnd_mode") ?: false
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
            .collection("preferences")
            .document("notifications")
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                // Successfully updated
            }
            .addOnFailureListener {
                showToast("Failed to update preference: $key")
            }
    }

    /**
     * Disable Do Not Disturb mode automatically after 24 hours.
     */
    private fun deactivateDndAfter24Hours() {
        Handler(Looper.getMainLooper()).postDelayed({
            dndSwitch.isChecked = false
            updatePreference("dnd_mode", false)
            showToast("Do Not Disturb Mode Disabled Automatically")
        }, 24 * 60 * 60 * 1000) // 24 hours in milliseconds
    }

    /**
     * Display a short toast message.
     */
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
