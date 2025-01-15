package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tourniverse.R
import com.example.tourniverse.viewmodels.NotificationsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsFragment : Fragment() {

    private lateinit var pushSwitch: Switch
    private lateinit var scoresSwitch: Switch
    private lateinit var chatSwitch: Switch
    private lateinit var commentsSwitch: Switch
    private lateinit var likesSwitch: Switch

    private lateinit var viewModel: NotificationsViewModel
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)

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
            // Load preferences using ViewModel
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
     * Load user preferences using the ViewModel.
     */
    private fun loadPreferences() {
        viewModel.loadPreferences(
            userId,
            onSuccess = { preferences ->
                pushSwitch.isChecked = preferences["Push"] ?: true
                scoresSwitch.isChecked = preferences["Scores"] ?: true
                chatSwitch.isChecked = preferences["ChatMessages"] ?: true
                commentsSwitch.isChecked = preferences["Comments"] ?: true
                likesSwitch.isChecked = preferences["Likes"] ?: true
            },
            onError = { error ->
                showToast(error)
            }
        )
    }

    /**
     * Update a specific user preference in Firebase Firestore.
     */
    private fun updatePreference(key: String, value: Boolean) {
        val updates = mapOf(key to value)

        FirebaseFirestore.getInstance()
            .collection("users")
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
