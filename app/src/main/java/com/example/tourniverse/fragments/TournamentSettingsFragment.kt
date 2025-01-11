package com.example.tourniverse.fragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
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
import com.example.tourniverse.activities.MainActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.dynamicLinks
import com.google.firebase.dynamiclinks.shortLinkAsync
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
            shareTournamentInvite() // Calls the updated invite function
        }

        // Adjust visibility based on ownership
        checkButtonVisibility()
    }

    private fun checkButtonVisibility() {
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                val ownerId = document.getString("ownerId")
                val privacy = document.getString("privacy") ?: "public"

                if (ownerId == userId) {
                    buttonDeleteTournament.visibility = View.VISIBLE
                    buttonLeaveTournament.visibility = View.GONE // Owner sees only Delete
                    buttonInvite.visibility = View.VISIBLE

                } else {
                    buttonLeaveTournament.visibility = View.VISIBLE
                    buttonDeleteTournament.visibility = View.GONE // Non-owners see only Leave

                    if(privacy == "private") {
                        buttonInvite.visibility = View.GONE
                    }else{
                        buttonInvite.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "Error checking button visibility: ${e.message}")
            }
    }

    private fun leaveTournament() {
        // Redirect user to Home Page first
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        activity?.finish()

        // Remove user from tournament's viewers list
        db.collection("tournaments").document(tournamentId)
            .update("viewers", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                // Remove tournament from user's viewed tournaments
                db.collection("users").document(userId)
                    .update("viewedTournaments", FieldValue.arrayRemove(tournamentId))
                    .addOnSuccessListener {
                        Log.d("TournamentSettings", "User removed successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("TournamentSettings", "Failed to remove user from their tournaments: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "Failed to remove user from tournament viewers: ${e.message}")
            }
    }


    private fun deleteTournament() {
        // Redirect user to Home Page first
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        activity?.finish()

        // Fetch all viewers and owner ID
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                val viewers = document.get("viewers") as? List<String> ?: emptyList()
                val ownerId = document.getString("ownerId") ?: ""

                // Remove the tournament ID from all viewers' viewedTournaments
                for (viewerId in viewers) {
                    db.collection("users").document(viewerId)
                        .update("viewedTournaments", FieldValue.arrayRemove(tournamentId))
                        .addOnSuccessListener {
                            Log.d("TournamentSettings", "Removed tournament from viewer: $viewerId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("TournamentSettings", "Failed to remove tournament from viewer $viewerId: ${e.message}")
                        }
                }

                // Remove the tournament ID from the owner's ownedTournaments
                db.collection("users").document(ownerId)
                    .update("ownedTournaments", FieldValue.arrayRemove(tournamentId))
                    .addOnSuccessListener {
                        Log.d("TournamentSettings", "Removed tournament from owner's ownedTournaments.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("TournamentSettings", "Failed to remove tournament from owner's ownedTournaments: ${e.message}")
                    }

                // Delete subcollections first
                deleteSubcollectionsAndDocument("tournaments", tournamentId)
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "Failed to fetch tournament details: ${e.message}")
            }
    }

    /**
     * Recursively deletes all subcollections and the parent document.
     */
    private fun deleteSubcollectionsAndDocument(collectionPath: String, documentId: String) {
        val documentRef = db.collection(collectionPath).document(documentId)

        documentRef.collection("chat").get().addOnSuccessListener { chatSnapshots ->
            for (doc in chatSnapshots) {
                doc.reference.delete()
            }

            documentRef.collection("matches").get().addOnSuccessListener { matchSnapshots ->
                for (doc in matchSnapshots) {
                    doc.reference.delete()
                }

                documentRef.collection("standings").get().addOnSuccessListener { standingsSnapshots ->
                    for (doc in standingsSnapshots) {
                        doc.reference.delete()
                    }

                    // Finally, delete the main document after subcollections are cleared
                    documentRef.delete()
                        .addOnSuccessListener {
                            Log.d("TournamentSettings", "Tournament and subcollections deleted successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("TournamentSettings", "Failed to delete tournament: ${e.message}")
                        }
                }
            }
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

    private fun shareTournamentInvite() {
        // Fetch the tournament name from Firestore
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val tournamentName = document.getString("name") ?: "Tournament"
                    val inviteMessage = "Hey, join the tournament \"$tournamentName\" using this code: $tournamentId"

                    // Open the share dialog
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, inviteMessage)
                    }
                    startActivity(Intent.createChooser(intent, "Share Tournament Code"))
                } else {
                    Toast.makeText(context, "Tournament not found!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error retrieving tournament: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
