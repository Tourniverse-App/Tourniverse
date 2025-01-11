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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.activities.MainActivity
import com.example.tourniverse.adapters.MembersAdapter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.dynamicLinks
import com.google.firebase.dynamiclinks.shortLinkAsync
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.tourniverse.models.User

class TournamentSettingsFragment : Fragment() {

    private lateinit var buttonMembers: Button
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
        tournamentId = arguments?.getString("tournamentId") ?: ""
        if (tournamentId.isEmpty()) {
            Log.e("TournamentSettings", "tournamentId is null or empty")
            Toast.makeText(context, "Invalid tournament ID", Toast.LENGTH_SHORT).show()
            activity?.onBackPressed() // Navigate back or handle gracefully
            return
        }

        // Initialize views
        switchGameNotifications = view.findViewById(R.id.switch_game_notifications)
        switchSocialNotifications = view.findViewById(R.id.switch_social_notifications)
        buttonLeaveTournament = view.findViewById(R.id.button_leave_tournament)
        buttonInvite = view.findViewById(R.id.button_invite)
        buttonDeleteTournament = view.findViewById(R.id.button_delete_tournament)

        // Set default visibility for buttons
        buttonDeleteTournament.visibility = View.GONE
        buttonLeaveTournament.visibility = View.GONE
        buttonInvite.visibility = View.GONE

        // Load user settings
        loadSettings(userId, tournamentId)

        buttonMembers = view.findViewById(R.id.button_members)
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                val memberCount = document.getLong("memberCount")?.toInt() ?: 0
                buttonMembers.text = "$memberCount Members"

                buttonMembers.setOnClickListener {
                    showMembersPopup()
                }
            }

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

                    buttonInvite.visibility = if (privacy == "private") View.GONE else View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "Error checking button visibility: ${e.message}")
                Toast.makeText(context, "Failed to load tournament data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun leaveTournament() {
        // Redirect user to Home Page first
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        activity?.finish()

        // Remove user from tournament's viewers list and decrement the member count
        db.collection("tournaments").document(tournamentId)
            .update(
                "viewers", FieldValue.arrayRemove(userId),
                "memberCount", FieldValue.increment(-1) // Decrement the member count
            )
            .addOnSuccessListener {
                // Remove tournament from user's viewed tournaments
                db.collection("users").document(userId)
                    .update("viewedTournaments", FieldValue.arrayRemove(tournamentId))
                    .addOnSuccessListener {
                        Log.d("TournamentSettings", "User removed successfully and member count updated.")
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
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val gameNotifications = document.getBoolean("gameNotifications") ?: false
                    val socialNotifications = document.getBoolean("socialNotifications") ?: false
                    switchGameNotifications.isChecked = gameNotifications
                    switchSocialNotifications.isChecked = socialNotifications
                } else {
                    Log.e("TournamentSettings", "No settings document found")
                    Toast.makeText(context, "Settings not available for this tournament", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "Error loading settings: ${e.message}")
                Toast.makeText(context, "Failed to load settings", Toast.LENGTH_SHORT).show()
            }
    }

    private fun shareTournamentInvite() {
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
                Log.e("TournamentSettings", "Error retrieving tournament: ${e.message}")
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

    lateinit var membersAdapter: MembersAdapter

    private fun showMembersPopup() {
        val membersPopup = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_members, null)
        val recyclerView = membersPopup.findViewById<RecyclerView>(R.id.recycler_members)

        val members = mutableListOf<User>()

        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Toast.makeText(requireContext(), "Tournament not found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val ownerId = document.getString("ownerId") ?: return@addOnSuccessListener
                val viewerIds = document.get("viewers") as? List<String> ?: emptyList()

                db.collection("users").document(ownerId).get()
                    .addOnSuccessListener { ownerDocument ->
                        val ownerName = ownerDocument.getString("username") ?: "Unknown Owner"
                        members.add(User(userId = ownerId, name = ownerName, email = ""))

                        fetchViewerDetails(viewerIds, members) { finalMembers ->
                            membersAdapter = MembersAdapter(
                                tournamentId = tournamentId,
                                isOwner = userId == ownerId,
                                fragment = this,
                                ownerId = ownerId
                            )
                            recyclerView.adapter = membersAdapter
                            recyclerView.layoutManager = LinearLayoutManager(requireContext())
                            membersAdapter.submitList(finalMembers.toMutableList())
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to load owner details.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load tournament data.", Toast.LENGTH_SHORT).show()
            }

        AlertDialog.Builder(requireContext())
            .setView(membersPopup)
            .setPositiveButton("Close") { _, _ ->
                refreshTournamentSettings()
            }
            .show()
    }

    /**
     * Fetch viewer details by iterating over the viewer IDs.
     */
    private fun fetchViewerDetails(
        viewerIds: List<String>,
        members: MutableList<User>,
        onComplete: (List<User>) -> Unit
    ) {
        if (viewerIds.isEmpty()) {
            onComplete(members)
            return
        }

        val db = FirebaseFirestore.getInstance()
        val tasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()

        for (viewerId in viewerIds) {
            val task = db.collection("users").document(viewerId).get()
                .addOnSuccessListener { viewerDocument ->
                    val viewerName = viewerDocument.getString("username") ?: "Unknown Viewer"
                    members.add(User(userId = viewerId, name = viewerName, email = ""))
                }
                .addOnFailureListener {
                    Log.e("fetchViewerDetails", "Failed to fetch viewer details for ID: $viewerId")
                }
            tasks.add(task)
        }

        com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
            .addOnSuccessListener {
                onComplete(members)
            }
            .addOnFailureListener {
                onComplete(members)
            }
    }

    fun refreshMembersPopup() {
        showMembersPopup() // Reopen the popup with updated members list
    }

    fun refreshTournamentSettings() {
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                val memberCount = document.getLong("memberCount")?.toInt() ?: 0
                buttonMembers.text = "$memberCount Members"
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to refresh tournament settings", Toast.LENGTH_SHORT).show()
            }
    }
}
