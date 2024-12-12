package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.tourniverse.R

class HelpSupportFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_help_support, container, false)

        // FAQ Click Listener
        view.findViewById<LinearLayout>(R.id.faqLayout).setOnClickListener {
            showPopupDialog("FAQ", getString(R.string.faq_text))
        }

        // Contact Support Click Listener
        view.findViewById<LinearLayout>(R.id.contactSupportLayout).setOnClickListener {
            showPopupDialog("Contact Support", getString(R.string.contact_support_text))
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
