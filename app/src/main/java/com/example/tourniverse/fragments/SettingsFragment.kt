package com.example.tourniverse.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tourniverse.activities.LoginActivity
import com.example.tourniverse.R
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Account Settings Click Listener
        view.findViewById<TextView>(R.id.accountSettings).setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_accountFragment)
        }

        // Notification Settings Click Listener
        view.findViewById<TextView>(R.id.notificationSettings).setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_notificationFragment)
        }

        // Help & Support Click Listener
        view.findViewById<TextView>(R.id.helpSupport).setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_helpSupportFragment)
        }

        // About Click Listener
        view.findViewById<TextView>(R.id.aboutSettings).setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_aboutFragment)
        }

        // Custom Sign Out Button
        val signOutBtn = view.findViewById<View>(R.id.signOutBtn)
        val signOutIcon = view.findViewById<View>(R.id.signOutIcon)
        val signOutText = view.findViewById<TextView>(R.id.signOutText)

        signOutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            Toast.makeText(context, "Signed Out Successfully", Toast.LENGTH_SHORT).show()
        }

        // Handle hover and animation effects for the sign-out button
        signOutBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Expand the button to reveal text
                    signOutBtn.animate().scaleX(2.8f).setDuration(300).start()
                    signOutText.visibility = View.VISIBLE
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Shrink the button back to circle and hide text
                    signOutBtn.animate().scaleX(1f).setDuration(300).start()
                    signOutText.visibility = View.GONE
                }
            }
            false
        }

        return view
    }
}
