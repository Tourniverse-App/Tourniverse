package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tourniverse.R

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Account Settings Click Listener
        view.findViewById<TextView>(R.id.accountSettings).setOnClickListener {
            Toast.makeText(context, "Account Settings Clicked", Toast.LENGTH_SHORT).show()
        }

        // Notification Settings Click Listener
        view.findViewById<TextView>(R.id.notificationSettings).setOnClickListener {
            Toast.makeText(context, "Notification Settings Clicked", Toast.LENGTH_SHORT).show()
        }

        // Privacy Settings Click Listener
        view.findViewById<TextView>(R.id.privacySettings).setOnClickListener {
            Toast.makeText(context, "Privacy & Security Clicked", Toast.LENGTH_SHORT).show()
        }

        // Help & Support Click Listener
        view.findViewById<TextView>(R.id.helpSupport).setOnClickListener {
            Toast.makeText(context, "Help & Support Clicked", Toast.LENGTH_SHORT).show()
        }

        // About Click Listener
        view.findViewById<TextView>(R.id.aboutSettings).setOnClickListener {
            Toast.makeText(context, "About Clicked", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
