package com.example.tournamentapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tourniverse.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class TournamentSettingsFragment : Fragment() {

    private lateinit var switchGameNotifications: Switch
    private lateinit var switchSocialNotifications: Switch
    private lateinit var buttonLeaveTournament: Button

    private val db = FirebaseFirestore.getInstance()
    private val userId: String by lazy { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    private lateinit var tournamentId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        return inflater.inflate(R.layout.fragment_tournament_settings, container, false)
    }

    /**
     * Validates the tournamentId passed via arguments and initializes the settings screen.
     * If the tournamentId is invalid, an error message is shown, and the fragment does not proceed.
     *
     * @param view The root view of the fragment.
     * @param savedInstanceState Saved state of the fragment.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tournamentId = arguments?.getString("tournamentId") ?: run {
            Toast.makeText(context, "Invalid tournament ID", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize views and load settings
        switchGameNotifications = view.findViewById(R.id.switch_game_notifications)
        switchSocialNotifications = view.findViewById(R.id.switch_social_notifications)
        buttonLeaveTournament = view.findViewById(R.id.button_leave_tournament)

        loadSettings()

        // Listeners
        switchGameNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationSetting("gameNotifications", isChecked)
        }

        switchSocialNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationSetting("socialNotifications", isChecked)
        }

        buttonLeaveTournament.setOnClickListener {
            leaveTournament()
        }
    }


    private fun loadSettings() {
        // Fetch existing settings from Firebase
        db.collection("users").document(userId)
            .collection("tournamentSettings").document(tournamentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    switchGameNotifications.isChecked = document.getBoolean("gameNotifications") ?: false
                    switchSocialNotifications.isChecked = document.getBoolean("socialNotifications") ?: false
                }
            }
    }

    private fun updateNotificationSetting(field: String, value: Boolean) {
        db.collection("users").document(userId)
            .collection("tournamentSettings").document(tournamentId)
            .update(field, value)
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun leaveTournament() {
        db.collection("users").document(userId)
            .update("tournaments", FieldValue.arrayRemove(tournamentId))
            .addOnSuccessListener {
                Toast.makeText(context, "You have left the tournament.", Toast.LENGTH_SHORT).show()
                activity?.finish() // Close fragment
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
