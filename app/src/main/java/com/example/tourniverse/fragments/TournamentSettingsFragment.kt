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
import com.example.tourniverse.utils.FirebaseHelper

class TournamentSettingsFragment : Fragment() {

    private lateinit var buttonMembers: Button
    private lateinit var switchPushNotifications: Switch
    private lateinit var switchScoresNotifications: Switch
    private lateinit var switchChatNotifications: Switch
    private lateinit var switchCommentsNotifications: Switch
    private lateinit var switchLikesNotifications: Switch
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
        switchPushNotifications = view.findViewById(R.id.switchPushNotifications)
        switchScoresNotifications = view.findViewById(R.id.switchScoresNotifications)
        switchChatNotifications = view.findViewById(R.id.switchChatNotifications)
        switchCommentsNotifications = view.findViewById(R.id.switchCommentsNotifications)
        switchLikesNotifications = view.findViewById(R.id.switchLikesNotifications)
        buttonLeaveTournament = view.findViewById(R.id.button_leave_tournament)
        buttonInvite = view.findViewById(R.id.button_invite)
        buttonDeleteTournament = view.findViewById(R.id.button_delete_tournament)

        // Set default visibility for buttons
        buttonDeleteTournament.visibility = View.GONE
        buttonLeaveTournament.visibility = View.GONE
        buttonInvite.visibility = View.GONE

        // Load user settings
        loadTournamentNotificationSettings(userId, tournamentId)

        buttonMembers = view.findViewById(R.id.button_members)
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                val memberCount = document.getLong("memberCount")?.toInt() ?: 0
                buttonMembers.text = "$memberCount Members"

                buttonMembers.setOnClickListener {
                    showMembersPopup()
                }
            }

        // Load settings
        loadTournamentNotificationSettings(userId, tournamentId)

        // Set listeners for notification switches
        switchPushNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationSetting("Push", isChecked)
            showToast("Push Notifications ${if (isChecked) "Enabled" else "Disabled"}")
        }

        // Set listeners for notification switches
        switchPushNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (switchPushNotifications.isEnabled) {
                updateNotificationSetting("push", isChecked)
                showToast("Push Notifications ${if (isChecked) "Enabled" else "Disabled"}")
            } else {
                showToast("Push Notifications are disabled in the global settings.")
                switchPushNotifications.isChecked = false
            }
        }

        switchScoresNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (switchScoresNotifications.isEnabled) {
                updateNotificationSetting("Scores", isChecked)
                showToast("Scores Notifications ${if (isChecked) "Enabled" else "Disabled"}")
            } else {
                showToast("Scores Notifications are disabled in the global settings.")
                switchScoresNotifications.isChecked = false
            }
        }

        switchChatNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (switchChatNotifications.isEnabled) {
                updateNotificationSetting("ChatMessages", isChecked)
                showToast("Chat Notifications ${if (isChecked) "Enabled" else "Disabled"}")
            } else {
                showToast("Chat Notifications are disabled in the global settings.")
                switchChatNotifications.isChecked = false
            }
        }

        switchCommentsNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (switchCommentsNotifications.isEnabled) {
                updateNotificationSetting("Comments", isChecked)
                showToast("Comments Notifications ${if (isChecked) "Enabled" else "Disabled"}")
            } else {
                showToast("Comments Notifications are disabled in the global settings.")
                switchCommentsNotifications.isChecked = false
            }
        }

        switchLikesNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (switchLikesNotifications.isEnabled) {
                updateNotificationSetting("Likes", isChecked)
                showToast("Likes Notifications ${if (isChecked) "Enabled" else "Disabled"}")
            } else {
                showToast("Likes Notifications are disabled in the global settings.")
                switchLikesNotifications.isChecked = false
            }
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
                FirebaseHelper.deleteTournament(requireContext(), tournamentId, false) // Redirect to home
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
                // Remove tournament from user's tournaments subcollection
                db.collection("users").document(userId)
                    .collection("tournaments").document(tournamentId).delete()
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
        intent.putExtra("REFRESH_HOME", true)
    }


    /**
     * Update a specific notification setting in Firestore.
     */
    private fun updateNotificationSetting(field: String, value: Boolean) {
        db.collection("users").document(userId)
            .collection("tournaments").document(tournamentId)
            .update(field, value)
            .addOnSuccessListener {
                Log.d("TournamentSettings", "Notification setting updated: $field = $value")
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "Failed to update notification setting: $field, Error: ${e.message}")
                Toast.makeText(context, "Failed to update notification setting for $field", Toast.LENGTH_SHORT).show()
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

    /**
     * Load tournament-specific notification settings, respecting global restrictions.
     */
    private fun loadTournamentNotificationSettings(userId: String, tournamentId: String) {
        loadGlobalSettings { globalSettings ->
            db.collection("users").document(userId)
                .collection("tournaments").document(tournamentId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        switchPushNotifications.isChecked =
                            (globalSettings["Push"] == true) && (document.getBoolean("Push") ?: true)
                        switchPushNotifications.isEnabled = globalSettings["Push"] == true

                        switchScoresNotifications.isChecked =
                            (globalSettings["Scores"] == true) && (document.getBoolean("Scores") ?: true)
                        switchScoresNotifications.isEnabled = globalSettings["Scores"] == true

                        switchChatNotifications.isChecked =
                            (globalSettings["ChatMessages"] == true) && (document.getBoolean("ChatMessages") ?: false)
                        switchChatNotifications.isEnabled = globalSettings["ChatMessages"] == true

                        switchCommentsNotifications.isChecked =
                            (globalSettings["Comments"] == true) && (document.getBoolean("Comments") ?: false)
                        switchCommentsNotifications.isEnabled = globalSettings["Comments"] == true

                        switchLikesNotifications.isChecked =
                            (globalSettings["Likes"] == true) && (document.getBoolean("Likes") ?: false)
                        switchLikesNotifications.isEnabled = globalSettings["Likes"] == true
                    } else {
                        Log.e("TournamentSettings", "No notification settings document found")
                        Toast.makeText(context, "Settings not available for this tournament", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TournamentSettings", "Error loading notification settings: ${e.message}")
                    Toast.makeText(context, "Failed to load notification settings", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Load global notification settings and apply restrictions in the tournament settings.
     */
    private fun loadGlobalSettings(callback: (Map<String, Boolean>) -> Unit) {
        db.collection("users").document(userId)
            .collection("notifications").document("settings")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val globalSettings = mapOf(
                        "push" to (document.getBoolean("Push") ?: true),
                        "Scores" to (document.getBoolean("Scores") ?: true),
                        "ChatMessages" to (document.getBoolean("ChatMessages") ?: true),
                        "Comments" to (document.getBoolean("Comments") ?: true),
                        "Likes" to (document.getBoolean("Likes") ?: true),
                    )
                    callback(globalSettings)
                } else {
                    Log.e("TournamentSettings", "Global settings not found")
                    callback(emptyMap()) // Default to all enabled
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "Error loading global settings: ${e.message}")
                callback(emptyMap()) // Default to all enabled
            }
    }


    /**
     * Show a short toast message.
     */
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
