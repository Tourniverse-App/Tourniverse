package com.example.tourniverse.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tourniverse.R

class NotificationsFragment : Fragment() {

    private lateinit var pushSwitch: Switch
    private lateinit var emailSwitch: Switch
    private lateinit var smsSwitch: Switch
    private lateinit var dndSwitch: Switch

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        // Initialize switches
        pushSwitch = view.findViewById(R.id.switchPushNotifications)
        emailSwitch = view.findViewById(R.id.switchEmailNotifications)
        smsSwitch = view.findViewById(R.id.switchSmsNotifications)
        dndSwitch = view.findViewById(R.id.switchDndMode)

        // Load preferences
        val sharedPreferences = requireActivity().getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        pushSwitch.isChecked = sharedPreferences.getBoolean("push_notifications", true)
        emailSwitch.isChecked = sharedPreferences.getBoolean("email_notifications", true)
        smsSwitch.isChecked = sharedPreferences.getBoolean("sms_notifications", true)
        dndSwitch.isChecked = sharedPreferences.getBoolean("dnd_mode", false)

        // Set listeners for switches
        pushSwitch.setOnCheckedChangeListener { _, isChecked ->
            savePreference(sharedPreferences, "push_notifications", isChecked)
            Toast.makeText(context, "Push Notifications ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        emailSwitch.setOnCheckedChangeListener { _, isChecked ->
            savePreference(sharedPreferences, "email_notifications", isChecked)
            Toast.makeText(context, "Email Notifications ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        smsSwitch.setOnCheckedChangeListener { _, isChecked ->
            savePreference(sharedPreferences, "sms_notifications", isChecked)
            Toast.makeText(context, "SMS Notifications ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        dndSwitch.setOnCheckedChangeListener { _, isChecked ->
            savePreference(sharedPreferences, "dnd_mode", isChecked)
            Toast.makeText(context, "Do Not Disturb Mode ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun savePreference(sharedPreferences: android.content.SharedPreferences, key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }
}
