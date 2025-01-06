package com.example.tourniverse.fragments

import android.app.AlertDialog
import android.content.Intent
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
    private lateinit var buttonInvite: Button
    private lateinit var buttonDeleteTournament: Button

    private val db = FirebaseFirestore.getInstance()
    private val userId: String by lazy { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    private lateinit var tournamentId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tournament_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve tournament ID
        tournamentId = arguments?.getString("tournamentId") ?: run {
            Toast.makeText(context, "Invalid tournament ID", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize views
        switchGameNotifications = view.findViewById(R.id.switch_game_notifications)
        switchSocialNotifications = view.findViewById(R.id.switch_social_notifications)
        buttonLeaveTournament = view.findViewById(R.id.button_leave_tournament)
        buttonInvite = view.findViewById(R.id.button_invite)
        buttonDeleteTournament = view.findViewById(R.id.button_delete_tournament)

        // Load user settings
        loadSettings(userId, tournamentId)

        // Notification switch listeners
        switchGameNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationSetting("gameNotifications", isChecked)
        }

        switchSocialNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationSetting("socialNotifications", isChecked)
        }

        // Leave Tournament button with confirmation dialog
        buttonLeaveTournament.setOnClickListener {
            showConfirmationDialog("Leave Tournament") {
                leaveTournament() // Executes only if confirmed
            }
        }

        // Delete Tournament button with confirmation dialog
        buttonDeleteTournament.setOnClickListener {
            showConfirmationDialog("Delete Tournament") {
                deleteTournament() // Executes only if confirmed
            }
        }

        // Invite button to generate or reuse invite link
        buttonInvite.setOnClickListener {
            generateInviteLink()
        }

        // Adjust visibility based on ownership
        checkButtonVisibility()
    }


    private fun checkButtonVisibility() {
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                val ownerId = document.getString("ownerId")

                if (ownerId == userId) {
                    buttonDeleteTournament.visibility = View.VISIBLE
                    buttonLeaveTournament.visibility = View.GONE // Owner sees only Delete
                } else {
                    buttonLeaveTournament.visibility = View.VISIBLE
                    buttonDeleteTournament.visibility = View.GONE // Non-owners see only Leave
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "Error checking button visibility: ${e.message}")
            }
    }



    private fun inviteToTournament() {
        val inviteMessage = "Join my tournament on Tourniverse! It's a fun competition format with teams. Don't miss out!"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, inviteMessage)
        }

        startActivity(Intent.createChooser(intent, "Invite via"))
    }

    private fun leaveTournament() {
        db.collection("users").document(userId)
            .update("tournaments", FieldValue.arrayRemove(tournamentId))
            .addOnSuccessListener {
                Toast.makeText(context, "You have left the tournament.", Toast.LENGTH_SHORT).show()
                activity?.finish()
            }
    }

    private fun deleteTournament() {
        db.collection("tournaments").document(tournamentId).delete()
            .addOnSuccessListener {
                db.collection("users").get().addOnSuccessListener { users ->
                    for (user in users) {
                        user.reference.update("tournaments", FieldValue.arrayRemove(tournamentId))
                    }
                }
                Toast.makeText(context, "Tournament deleted successfully.", Toast.LENGTH_SHORT).show()
                activity?.finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete tournament: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateNotificationSetting(field: String, value: Boolean) {
        db.collection("users").document(userId)
            .collection("tournamentSettings").document(tournamentId)
            .update(field, value)
    }

    private fun loadSettings(userId: String, tournamentId: String) {
        db.collection("users").document(userId)
            .collection("tournamentSettings").document(tournamentId).get()
    }

    private fun generateInviteLink() {
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                val existingLink = document.getString("link") // Check if a link already exists
                val link = existingLink ?: "https://tourniverse.app/join?tournamentId=$tournamentId" // Use existing or create new

                if (existingLink == null) { // Create link if it doesn't exist
                    db.collection("tournaments").document(tournamentId)
                        .update("link", link)
                        .addOnSuccessListener {
                            shareInviteLink(link)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to create invite link: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    shareInviteLink(link) // Share existing link
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching tournament details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Opens share dialog for the invite link.
     */
    private fun shareInviteLink(link: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Join my tournament: $link")
        }
        startActivity(Intent.createChooser(intent, "Invite via"))
    }

    private fun showConfirmationDialog(action: String, callback: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("$action Confirmation")
            .setMessage("Are you sure you want to $action?")
            .setPositiveButton("Yes") { _, _ -> callback() }
            .setNegativeButton("No", null)
            .show()
    }


}
