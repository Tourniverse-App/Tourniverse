package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tourniverse.R
import com.example.tourniverse.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth

class AddTournamentFragment : Fragment() {

    private lateinit var etTournamentName: EditText
    private lateinit var spinnerTournamentType: Spinner
    private lateinit var spinnerNumTeams: Spinner
    private lateinit var layoutTeamNames: LinearLayout
    private lateinit var spinnerPrivacy: Spinner
    private lateinit var etDescription: EditText
    private lateinit var btnSubmitTournament: Button

    private var currentType: String = "Tables"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_tournament, container, false)

        // Initialize views
        etTournamentName = view.findViewById(R.id.etTournamentName)
        spinnerTournamentType = view.findViewById(R.id.spinnerTournamentType)
        spinnerNumTeams = view.findViewById(R.id.spinnerNumTeams)
        layoutTeamNames = view.findViewById(R.id.layoutTeamNames)
        spinnerPrivacy = view.findViewById(R.id.spinnerPrivacy)
        etDescription = view.findViewById(R.id.etDescription)
        btnSubmitTournament = view.findViewById(R.id.btnSubmitTournament)

        setupTournamentTypeSpinner()
        setupDefaultPrivacySpinner()

        btnSubmitTournament.setOnClickListener {
            handleSubmit()
        }

        return view
    }

    private fun setupTournamentTypeSpinner() {
        val typeOptions = listOf("Tables", "Knockout")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, typeOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTournamentType.adapter = adapter

        spinnerTournamentType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentType = typeOptions[position]
                setupNumberOfTeamsSpinner()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupNumberOfTeamsSpinner() {
        val teamOptions = if (currentType == "Tables") {
            (2..32).toList()
        } else {
            listOf(4, 8, 16, 32)
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, teamOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNumTeams.adapter = adapter

        spinnerNumTeams.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val numTeams = teamOptions[position]
                showTeamNameFields(numTeams)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDefaultPrivacySpinner() {
        val privacyOptions = listOf("Public", "Private")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, privacyOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrivacy.adapter = adapter
        spinnerPrivacy.setSelection(1) // Default to "Private"
    }

    private fun showTeamNameFields(numTeams: Int) {
        layoutTeamNames.removeAllViews()
        for (i in 1..numTeams) {
            val editText = EditText(requireContext())
            editText.hint = "Team $i"
            editText.isSingleLine = true
            editText.inputType = EditorInfo.TYPE_CLASS_TEXT
            editText.imeOptions = EditorInfo.IME_ACTION_DONE

            editText.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutTeamNames.addView(editText)
        }
    }

    private fun handleSubmit() {
        val tournamentName = etTournamentName.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val privacy = spinnerPrivacy.selectedItem.toString()

        if (tournamentName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter the tournament name", Toast.LENGTH_SHORT).show()
            return
        }

        val teamNames = mutableListOf<String>()
        for (i in 0 until layoutTeamNames.childCount) {
            val teamField = layoutTeamNames.getChildAt(i) as EditText
            val teamName = teamField.text.toString().trim()
            if (teamName.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all the team names", Toast.LENGTH_SHORT).show()
                return
            }
            teamNames.add(teamName)
        }

        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a description", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the current user's UID
        val currentUser = FirebaseAuth.getInstance().currentUser
        val ownerId = currentUser?.uid ?: ""

        if (ownerId.isEmpty()) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable the button to prevent duplicate submissions
        btnSubmitTournament.isEnabled = false

        FirebaseHelper.addTournament(
            name = tournamentName,
            teamCount = teamNames.size,
            description = description,
            privacy = privacy,
            teamNames = teamNames,
            ownerId = ownerId
        ) { success, tournamentId ->
            if (success) {
                Toast.makeText(requireContext(), "Tournament saved successfully!", Toast.LENGTH_SHORT).show()
                tournamentId?.let {
                    val action = AddTournamentFragmentDirections.actionAddTournamentFragmentToTournamentDetailsFragment(it)
                    findNavController().navigate(action)
                }
            } else {
                Toast.makeText(requireContext(), "Error saving tournament", Toast.LENGTH_SHORT).show()
                btnSubmitTournament.isEnabled = true
            }
        }
    }
}
