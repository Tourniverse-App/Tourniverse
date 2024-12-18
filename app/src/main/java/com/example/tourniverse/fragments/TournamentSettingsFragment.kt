package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
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
        Log.d("TournamentSettings", "onCreateView: Inflating layout")
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

        Log.d("TournamentSettings", "onViewCreated: Starting setup")
        tournamentId = arguments?.getString("tournamentId") ?: run {
            Log.e("TournamentSettings", "onViewCreated: Tournament ID is null")
            Toast.makeText(context, "Invalid tournament ID", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("TournamentSettings", "onViewCreated: Tournament ID = $tournamentId")

        try {
            // Initialize views
            switchGameNotifications = view.findViewById(R.id.switch_game_notifications)
            switchSocialNotifications = view.findViewById(R.id.switch_social_notifications)
            buttonLeaveTournament = view.findViewById(R.id.button_leave_tournament)

            Log.d("TournamentSettings", "onViewCreated: Views initialized")

            loadSettings()

            // Listeners
            switchGameNotifications.setOnCheckedChangeListener { _, isChecked ->
                Log.d("TournamentSettings", "Game Notifications toggled: $isChecked")
                updateNotificationSetting("gameNotifications", isChecked)
            }

            switchSocialNotifications.setOnCheckedChangeListener { _, isChecked ->
                Log.d("TournamentSettings", "Social Notifications toggled: $isChecked")
                updateNotificationSetting("socialNotifications", isChecked)
            }

            buttonLeaveTournament.setOnClickListener {
                Log.d("TournamentSettings", "Leave Tournament button clicked")
                leaveTournament()
            }
        } catch (e: Exception) {
            Log.e("TournamentSettings", "onViewCreated: Exception occurred - ${e.message}", e)
            Toast.makeText(context, "Error initializing settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSettings() {
        Log.d("TournamentSettings", "loadSettings: Fetching settings for tournament: $tournamentId")
        db.collection("users").document(userId)
            .collection("tournamentSettings").document(tournamentId)
            .get()
            .addOnSuccessListener { document ->
                Log.d("TournamentSettings", "loadSettings: Document fetched - exists = ${document.exists()}")
                if (document.exists()) {
                    val gameNotifications = document.getBoolean("gameNotifications") ?: false
                    val socialNotifications = document.getBoolean("socialNotifications") ?: false
                    Log.d("TournamentSettings", "loadSettings: gameNotifications=$gameNotifications, socialNotifications=$socialNotifications")

                    switchGameNotifications.isChecked = gameNotifications
                    switchSocialNotifications.isChecked = socialNotifications
                } else {
                    Log.e("TournamentSettings", "loadSettings: No settings document found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "loadSettings: Failed to fetch settings - ${e.message}", e)
                Toast.makeText(context, "Error loading settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateNotificationSetting(field: String, value: Boolean) {
        Log.d("TournamentSettings", "updateNotificationSetting: Updating $field to $value")
        db.collection("users").document(userId)
            .collection("tournamentSettings").document(tournamentId)
            .update(field, value)
            .addOnSuccessListener {
                Log.d("TournamentSettings", "updateNotificationSetting: Successfully updated $field to $value")
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "updateNotificationSetting: Failed to update $field - ${e.message}", e)
                Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun leaveTournament() {
        Log.d("TournamentSettings", "leaveTournament: Removing user from tournament: $tournamentId")
        db.collection("users").document(userId)
            .update("tournaments", FieldValue.arrayRemove(tournamentId))
            .addOnSuccessListener {
                Log.d("TournamentSettings", "leaveTournament: Successfully left tournament")
                Toast.makeText(context, "You have left the tournament.", Toast.LENGTH_SHORT).show()
                activity?.finish()
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "leaveTournament: Failed to leave tournament - ${e.message}", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
