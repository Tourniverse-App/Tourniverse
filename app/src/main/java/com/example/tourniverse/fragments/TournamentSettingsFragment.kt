package com.example.tourniverse.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
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
    private lateinit var buttonLeaveTournament: Button
    private lateinit var buttonInvite: Button
    private lateinit var buttonDeleteTournament: Button

    private lateinit var customPushSwitch: View
    private lateinit var pushSliderCard: View

    private lateinit var customScoresSwitch: View
    private lateinit var scoresSliderCard: View

    private lateinit var customChatSwitch: View
    private lateinit var chatSliderCard: View

    private lateinit var customCommentsSwitch: View
    private lateinit var commentsSliderCard: View

    private lateinit var customLikesSwitch: View
    private lateinit var likesSliderCard: View

    private val db = FirebaseFirestore.getInstance()
    private val userId: String by lazy { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    private lateinit var tournamentId: String

    private var isGlobalPushEnabled = true

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

        // Initialize custom switch views
        customPushSwitch = view.findViewById(R.id.customPushSwitch)
        pushSliderCard = view.findViewById(R.id.customPushSliderCard)

        customScoresSwitch = view.findViewById(R.id.customScoresSwitch)
        scoresSliderCard = view.findViewById(R.id.customScoresSliderCard)

        customChatSwitch = view.findViewById(R.id.customChatSwitch)
        chatSliderCard = view.findViewById(R.id.customChatSliderCard)

        customCommentsSwitch = view.findViewById(R.id.customCommentsSwitch)
        commentsSliderCard = view.findViewById(R.id.customCommentsSliderCard)

        customLikesSwitch = view.findViewById(R.id.customLikesSwitch)
        likesSliderCard = view.findViewById(R.id.customLikesSliderCard)


        // Pass these variables to the loadTournamentNotificationSettings method
        loadTournamentNotificationSettings(
            pushSliderCard,
            customPushSwitch,
            scoresSliderCard,
            customScoresSwitch,
            chatSliderCard,
            customChatSwitch,
            commentsSliderCard,
            customCommentsSwitch,
            likesSliderCard,
            customLikesSwitch
        )

        buttonLeaveTournament = view.findViewById(R.id.button_leave_tournament)
        buttonInvite = view.findViewById(R.id.button_invite)
        buttonDeleteTournament = view.findViewById(R.id.button_delete_tournament)

        // Set default visibility for buttons
        buttonDeleteTournament.visibility = View.GONE
        buttonLeaveTournament.visibility = View.GONE
        buttonInvite.visibility = View.GONE

        buttonMembers = view.findViewById(R.id.button_members)
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                val memberCount = document.getLong("memberCount")?.toInt() ?: 0
                buttonMembers.text = "$memberCount Members"

                buttonMembers.setOnClickListener {
                    showMembersPopup()
                }
            }

        // Set up switch listeners with animations
        setupSwitchListener(customPushSwitch, pushSliderCard, "Push") { isChecked ->
            updateTournamentPreference("Push", isChecked)
            setDependentSwitchesEnabledUI(isChecked) // Update dependent switches
        }

        setupSwitchListener(customScoresSwitch, scoresSliderCard, "Scores") { isChecked ->
            updateTournamentPreference("Scores", isChecked)
        }

        setupSwitchListener(customChatSwitch, chatSliderCard, "ChatMessages") { isChecked ->
            updateTournamentPreference("ChatMessages", isChecked)
        }

        setupSwitchListener(customCommentsSwitch, commentsSliderCard, "Comments") { isChecked ->
            updateTournamentPreference("Comments", isChecked)
        }

        setupSwitchListener(customLikesSwitch, likesSliderCard, "Likes") { isChecked ->
            updateTournamentPreference("Likes", isChecked)
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

    /**
     * Sets up a switch listener with animations and state updates.
     */
    private fun setupSwitchListener(
        switch: View,
        sliderCard: View,
        preferenceKey: String,
        stateUpdater: (Boolean) -> Unit
    ) {
        var isChecked = false

        switch.setOnClickListener {
            if (!isGlobalPushEnabled) {
                // Use cached global push state
                showToast("Push notifications are disabled globally. Enable them in settings.")
            } else {
                try {
                    isChecked = !isChecked

                    // Animate slider
                    animateSlider(sliderCard, switch, isChecked)

                    // Delay state update to match animation
                    switch.postDelayed({
                        stateUpdater(isChecked)
                        updateTournamentPreference(preferenceKey, isChecked)
                        Log.d("TournamentSettings", "$preferenceKey toggled to: $isChecked")
                    }, 400)
                } catch (e: Exception) {
                    Log.e("TournamentSettings", "Error toggling $preferenceKey: ${e.message}")
                }
            }
        }
    }

    private fun setDependentSwitchesEnabledUI(isPushEnabled: Boolean) {
        val switches = listOf(
            Pair(customScoresSwitch, scoresSliderCard),
            Pair(customChatSwitch, chatSliderCard),
            Pair(customCommentsSwitch, commentsSliderCard),
            Pair(customLikesSwitch, likesSliderCard)
        )

        for ((switch, sliderCard) in switches) {
            switch.isClickable = isPushEnabled
            sliderCard.isClickable = isPushEnabled

            updateSwitchAppearance(sliderCard, switch, sliderCard.translationX != 0f, isPushEnabled)
        }
    }

    /**
     * Update a specific tournament notification setting in Firestore.
     */
    private fun updateTournamentPreference(field: String, value: Boolean) {
        db.collection("users").document(userId)
            .collection("tournaments").document(tournamentId)
            .update(field, value)
            .addOnSuccessListener {
                Log.d("TournamentSettings", "Preference updated: $field = $value")
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "Failed to update $field: ${e.message}")
                showToast("Failed to update $field setting.")
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
    private fun loadTournamentNotificationSettings(
        pushSliderCard: View,
        customPushSwitch: View,
        scoresSliderCard: View,
        customScoresSwitch: View,
        chatSliderCard: View,
        customChatSwitch: View,
        commentsSliderCard: View,
        customCommentsSwitch: View,
        likesSliderCard: View,
        customLikesSwitch: View
    ) {
        loadGlobalSettings { globalSettings ->
            if (!isGlobalPushEnabled) {
                // Global Push disabled; disable all switches
                applySwitchState(pushSliderCard, customPushSwitch, false)
                setDependentSwitchesEnabledUI(false)
                showToast("Push notifications are disabled globally. Enable them in settings.")
            } else {
                // Global Push enabled; load tournament-specific settings
                db.collection("users").document(userId)
                    .collection("tournaments").document(tournamentId).get()
                    .addOnSuccessListener { document ->
                        val isPushEnabled = document.getBoolean("Push") ?: false
                        val isScoresEnabled = document.getBoolean("Scores") ?: false
                        val isChatEnabled = document.getBoolean("ChatMessages") ?: false
                        val isCommentsEnabled = document.getBoolean("Comments") ?: false
                        val isLikesEnabled = document.getBoolean("Likes") ?: false

                        applySwitchState(pushSliderCard, customPushSwitch, isPushEnabled)
                        applySwitchState(scoresSliderCard, customScoresSwitch, isScoresEnabled)
                        applySwitchState(chatSliderCard, customChatSwitch, isChatEnabled)
                        applySwitchState(commentsSliderCard, customCommentsSwitch, isCommentsEnabled)
                        applySwitchState(likesSliderCard, customLikesSwitch, isLikesEnabled)

                        setDependentSwitchesEnabledUI(isPushEnabled) // Update dependent switches
                    }
                    .addOnFailureListener { e ->
                        Log.e("TournamentSettings", "Failed to load notification settings: ${e.message}")
                        Toast.makeText(context, "Failed to load settings.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun updateSwitchAppearance(sliderCard: View, switch: View, isChecked: Boolean, isEnabled: Boolean) {
        val translationX = if (isChecked) resources.getDimension(R.dimen.custom_switch_toggle) else 0f
        sliderCard.translationX = translationX

        val cardColor = if (!isEnabled) "#D3D3D3" else if (isChecked) "#379237" else "#DC3535"
        val switchColor = if (!isEnabled) "#E0E0E0" else if (isChecked) "#9ed99c" else "#f5aeae"

        (sliderCard.background as? GradientDrawable)?.setColor(android.graphics.Color.parseColor(cardColor))
        (switch.background as? GradientDrawable)?.setColor(android.graphics.Color.parseColor(switchColor))
    }

    private fun applySwitchState(sliderCard: View, slider: View, isChecked: Boolean) {
        // Set slider card translation
        val translationX = if (isChecked) resources.getDimension(R.dimen.custom_switch_toggle) else 0f
        sliderCard.translationX = translationX

        // Set slider card color
        val sliderCardBackground = sliderCard.background
        if (sliderCardBackground is GradientDrawable) {
            sliderCardBackground.setColor(
                android.graphics.Color.parseColor(if (isChecked) "#379237" else "#DC3535")
            )
        }

        // Set switch background color
        val sliderBackground = slider.background
        if (sliderBackground is GradientDrawable) {
            sliderBackground.setColor(
                android.graphics.Color.parseColor(if (isChecked) "#9ed99c" else "#f5aeae")
            )
        }
    }

    private fun animateSlider(sliderCard: View, slider: View, isChecked: Boolean) {
        try {
            val translationX = if (isChecked) resources.getDimension(R.dimen.custom_switch_toggle) else 0f
            val rotationAngle = if (isChecked) 0f else 180f

            // Move the slider
            val moveAnimator = ObjectAnimator.ofFloat(sliderCard, "translationX", translationX)

            // Rotate the slider-card faces
            val rotateAnimator = ObjectAnimator.ofFloat(sliderCard, "rotationY", sliderCard.rotationY, rotationAngle)

            // Update the slider card background color
            val sliderCardBackground = sliderCard.background
            if (sliderCardBackground is GradientDrawable) {
                sliderCardBackground.setColor(
                    android.graphics.Color.parseColor(if (isChecked) "#379237" else "#DC3535") // Green when on
                )
            }

            // Update the switch background color
            val sliderBackground = slider.background
            if (sliderBackground is GradientDrawable) {
                sliderBackground.setColor(
                    android.graphics.Color.parseColor(if (isChecked) "#9ed99c" else "#f5aeae")
                )
            }

            // Animator set for smooth animations
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(moveAnimator, rotateAnimator)
            animatorSet.duration = 400
            animatorSet.start()
        } catch (e: Exception) {
            Log.e("NotificationsFragment", "Error during animation: ${e.message}")
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
                    isGlobalPushEnabled = document.getBoolean("Push") ?: true // Cache global Push state
                    val globalSettings = mapOf(
                        "push" to isGlobalPushEnabled,
                        "Scores" to (document.getBoolean("Scores") ?: true),
                        "ChatMessages" to (document.getBoolean("ChatMessages") ?: true),
                        "Comments" to (document.getBoolean("Comments") ?: true),
                        "Likes" to (document.getBoolean("Likes") ?: true),
                    )
                    callback(globalSettings)
                } else {
                    Log.e("TournamentSettings", "Global settings not found")
                    isGlobalPushEnabled = true // Default to enabled
                    callback(emptyMap())
                }
            }
            .addOnFailureListener { e ->
                Log.e("TournamentSettings", "Error loading global settings: ${e.message}")
                isGlobalPushEnabled = true // Default to enabled
                callback(emptyMap())
            }
    }

    /**
     * Show a short toast message.
     */
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
