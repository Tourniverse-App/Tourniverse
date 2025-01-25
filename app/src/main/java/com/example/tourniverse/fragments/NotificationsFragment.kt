package com.example.tourniverse.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tourniverse.R
import com.example.tourniverse.viewmodels.NotificationsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class NotificationsFragment : Fragment() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("NotificationsFragment", "POST_NOTIFICATIONS permission granted.")
                enablePushSwitch() // Turn on Push Notifications
            } else {
                Log.d("NotificationsFragment", "POST_NOTIFICATIONS permission denied.")
                disablePushSwitch() // Keep Push Notifications off
            }
        }

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

    private var isPushEnabled = false
    private var isScoresEnabled = false
    private var isChatEnabled = false
    private var isCommentsEnabled = false
    private var isLikesEnabled = false

    private lateinit var userId: String
    private lateinit var viewModel: NotificationsViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        Log.d("NotificationsFragment", "Initializing ViewModel")
        viewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)

        // Get custom switch views
        try {
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

            Log.d("NotificationsFragment", "Switch views initialized successfully")
        } catch (e: Exception) {
            Log.e("NotificationsFragment", "Error initializing switch views: ${e.message}")
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            Log.d("NotificationsFragment", "User authenticated: $userId")
            loadPreferences()
        } else {
            showToast("User not authenticated")
            Log.e("NotificationsFragment", "User not authenticated")
        }


        setupSwitchListener(customScoresSwitch, scoresSliderCard, "Scores") { isScoresEnabled = it }
        setupSwitchListener(customChatSwitch, chatSliderCard, "ChatMessages") { isChatEnabled = it }
        setupSwitchListener(customCommentsSwitch, commentsSliderCard, "Comments") { isCommentsEnabled = it }
        setupSwitchListener(customLikesSwitch, likesSliderCard, "Likes") { isLikesEnabled = it }
        setupSwitchListener(customPushSwitch, pushSliderCard, "Push") { isEnabled ->
            if (isEnabled) {
                // User is trying to turn Push Notifications on
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("NotificationsFragment", "POST_NOTIFICATIONS permission already granted.")
                    isPushEnabled = true
                    setDependentSwitchesEnabledUI(true) // Enable other switches
                } else {
                    // Request POST_NOTIFICATIONS permission
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                // Push is being turned off, no need for permissions
                isPushEnabled = false
                setDependentSwitchesEnabledUI(false) // Gray out other switches
            }
        }

        return view
    }

    private fun loadPreferences() {
        viewModel.loadPreferences(
            userId,
            onSuccess = { preferences ->
                // Load and apply the preferences
                isPushEnabled = preferences["Push"] ?: false
                isScoresEnabled = preferences["Scores"] ?: false
                isChatEnabled = preferences["ChatMessages"] ?: false
                isCommentsEnabled = preferences["Comments"] ?: false
                isLikesEnabled = preferences["Likes"] ?: false

                // Set initial state and colors for all switches
                applySwitchState(pushSliderCard, customPushSwitch, isPushEnabled)
                applySwitchState(scoresSliderCard, customScoresSwitch, isScoresEnabled)
                applySwitchState(chatSliderCard, customChatSwitch, isChatEnabled)
                applySwitchState(commentsSliderCard, customCommentsSwitch, isCommentsEnabled)
                applySwitchState(likesSliderCard, customLikesSwitch, isLikesEnabled)

                // Gray out other switches if Push is off
                setDependentSwitchesEnabledUI(isPushEnabled)
            },
            onError = { error ->
                showToast(error)
            }
        )
    }

    private fun setupSwitchListener(
        switch: View,
        sliderCard: View,
        preferenceKey: String,
        stateUpdater: (Boolean) -> Unit
    ) {
        // Sync initial state with the slider's current position
        var isChecked = sliderCard.translationX != 0f // Assume 0f means unchecked

        // Apply the initial state to the slider visuals
        applySwitchState(sliderCard, switch, isChecked)

        switch.setOnClickListener {
            try {
                // Toggle state
                isChecked = !isChecked

                // Start the animation
                animateSlider(sliderCard, switch, isChecked)

                // Delay state update until animation completes
                switch.postDelayed({
                    stateUpdater(isChecked)
                    updatePreference(preferenceKey, isChecked) // Use updatePreference here
                    Log.d("NotificationsFragment", "$preferenceKey toggled to: $isChecked")
                }, 400) // Match animation duration
            } catch (e: Exception) {
                Log.e("NotificationsFragment", "Error toggling $preferenceKey: ${e.message}")
            }
        }
    }

    private fun enablePushSwitch() {
        isPushEnabled = true
        applySwitchState(pushSliderCard, customPushSwitch, true) // Visually turn on Push
        setDependentSwitchesEnabledUI(true) // Enable other switches
        updatePreference("Push", true) // Save preference to Firestore
    }

    private fun disablePushSwitch() {
        isPushEnabled = false
        applySwitchState(pushSliderCard, customPushSwitch, false) // Visually turn off Push
        setDependentSwitchesEnabledUI(false) // Gray out other switches
        updatePreference("Push", false) // Save preference to Firestore
    }

    private fun setDependentSwitchesEnabledUI(isPushEnabled: Boolean) {
        val switches = listOf(
            Pair(customScoresSwitch, scoresSliderCard),
            Pair(customChatSwitch, chatSliderCard),
            Pair(customCommentsSwitch, commentsSliderCard),
            Pair(customLikesSwitch, likesSliderCard)
        )

        for ((switch, sliderCard) in switches) {
            // Disable or enable clickability based on Push state
            switch.isClickable = isPushEnabled
            sliderCard.isClickable = isPushEnabled

            // Apply gray-out effect if Push is disabled
            val sliderCardColor = if (isPushEnabled) {
                if (sliderCard.translationX != 0f) "#379237" else "#DC3535" // Green or red based on state
            } else {
                "#D3D3D3" // Gray for disabled switches
            }

            val sliderBackgroundColor = if (isPushEnabled) {
                if (sliderCard.translationX != 0f) "#9ed99c" else "#f5aeae" // Light green/red based on state
            } else {
                "#E0E0E0" // Light gray for disabled switches
            }

            // Update slider card background
            val sliderCardBackground = sliderCard.background
            if (sliderCardBackground is GradientDrawable) {
                sliderCardBackground.setColor(android.graphics.Color.parseColor(sliderCardColor))
            }

            // Update switch background
            val sliderBackground = switch.background
            if (sliderBackground is GradientDrawable) {
                sliderBackground.setColor(android.graphics.Color.parseColor(sliderBackgroundColor))
            }
        }
    }

    private fun applySwitchState(sliderCard: View, slider: View, isChecked: Boolean) {
        val translationX = if (isChecked) resources.getDimension(R.dimen.custom_switch_toggle) else 0f
        sliderCard.translationX = translationX

        // Set colors for enabled or disabled states
        val sliderCardColor = if (slider.isClickable) {
            if (isChecked) "#379237" else "#DC3535" // Green when enabled, red when off
        } else {
            "#D3D3D3" // Gray for disabled switches
        }

        val sliderBackgroundColor = if (slider.isClickable) {
            if (isChecked) "#9ed99c" else "#f5aeae" // Light green/red when enabled
        } else {
            "#E0E0E0" // Light gray for disabled switches
        }

        // Update slider card background
        val sliderCardBackground = sliderCard.background
        if (sliderCardBackground is GradientDrawable) {
            sliderCardBackground.setColor(android.graphics.Color.parseColor(sliderCardColor))
        }

        // Update switch background
        val sliderBackground = slider.background
        if (sliderBackground is GradientDrawable) {
            sliderBackground.setColor(android.graphics.Color.parseColor(sliderBackgroundColor))
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

    private fun updatePreference(key: String, value: Boolean) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("notifications")
            .document("settings")
            .set(mapOf(key to value), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d("NotificationsFragment", "Preference updated: $key = $value")
            }
            .addOnFailureListener { e ->
                Log.e("NotificationsFragment", "Failed to update preference: ${e.message}")
                showToast("Failed to update preference: $key")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
