package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.tourniverse.R

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)

        // Set app version dynamically
        view.findViewById<TextView>(R.id.appVersion).text = "1.0.0"

        // Terms of Service Click Listener
        view.findViewById<LinearLayout>(R.id.termsOfServiceLayout).setOnClickListener {
            showPopupDialog("Terms of Service", getString(R.string.terms_of_service_text))
        }

        // Privacy Policy Click Listener
        view.findViewById<LinearLayout>(R.id.privacyPolicyLayout).setOnClickListener {
            showPopupDialog("Privacy Policy", getString(R.string.privacy_policy_text))
        }

        // Credits Click Listener
        view.findViewById<LinearLayout>(R.id.creditsLayout).setOnClickListener {
            showPopupDialog("Credits", getString(R.string.credits_text))
        }

        return view
    }

    private fun showPopupDialog(title: String, message: String) {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle(title)
        dialog.setMessage(message)
        dialog.setPositiveButton("Close") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        dialog.create().show()
    }
}
