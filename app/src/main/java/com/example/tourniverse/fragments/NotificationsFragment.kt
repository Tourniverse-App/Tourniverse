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

class NotificationsFragment : Fragment() {

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

        setupSwitchListener(customPushSwitch, pushSliderCard, "Push") { isPushEnabled = it }
        setupSwitchListener(customScoresSwitch, scoresSliderCard, "Scores") { isScoresEnabled = it }
        setupSwitchListener(customChatSwitch, chatSliderCard, "ChatMessages") { isChatEnabled = it }
        setupSwitchListener(customCommentsSwitch, commentsSliderCard, "Comments") { isCommentsEnabled = it }
        setupSwitchListener(customLikesSwitch, likesSliderCard, "Likes") { isLikesEnabled = it }

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
