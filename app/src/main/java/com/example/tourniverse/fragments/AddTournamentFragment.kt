package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tourniverse.R
import com.example.tourniverse.utils.FirebaseHelper

class AddTournamentFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_tournament, container, false)

        val nameField = view.findViewById<EditText>(R.id.etTournamentName)
        val teamCountSpinner = view.findViewById<Spinner>(R.id.spinnerTeams)
        val submitButton = view.findViewById<Button>(R.id.btnSubmitTournament)

        submitButton.setOnClickListener {
            val name = nameField.text.toString()
            val teamCount = teamCountSpinner.selectedItem.toString()

            if (name.isEmpty()) {
                Toast.makeText(context, "Tournament Name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseHelper.addTournament(name, teamCount.toInt())
        }

        return view
    }
}
