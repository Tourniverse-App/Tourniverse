package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.tourniverse.R
import com.example.tourniverse.models.Tournament

class AddTournamentFragment : Fragment() {

    private lateinit var spinnerNumTeams: Spinner
    private lateinit var layoutTeamNames: LinearLayout
    private lateinit var spinnerTournamentFormat: Spinner
    private lateinit var spinnerPrivacy: Spinner
    private lateinit var etTournamentName: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnSubmitTournament: Button

    private val tournamentsList = mutableListOf<Tournament>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_tournament, container, false)

        // Initialize Views
        spinnerNumTeams = view.findViewById(R.id.spinnerNumTeams)
        layoutTeamNames = view.findViewById(R.id.layoutTeamNames)
        spinnerTournamentFormat = view.findViewById(R.id.spinnerTournamentFormat)
        spinnerPrivacy = view.findViewById(R.id.spinnerPrivacy)
        etTournamentName = view.findViewById(R.id.etTournamentName)
        etDescription = view.findViewById(R.id.etDescription)
        btnSubmitTournament = view.findViewById(R.id.btnSubmitTournament)

        // Setup Spinners
        setupNumberOfTeamsSpinner()
        setupDefaultSpinners()

        // Handle Submit Button
        btnSubmitTournament.setOnClickListener {
            handleSubmit()
        }

        return view
    }

    private fun setupNumberOfTeamsSpinner() {
        val teamOptions = (2..32 step 2).toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, teamOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNumTeams.adapter = adapter

        // Set default to 6 teams
        val defaultIndex = teamOptions.indexOf(6)
        spinnerNumTeams.setSelection(defaultIndex)

        spinnerNumTeams.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val numTeams = teamOptions[position]
                showTeamNameFields(numTeams)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDefaultSpinners() {
        // Set default for Tournament Format to "Tables"
        val formatOptions = resources.getStringArray(R.array.tournament_format).toList()
        val defaultFormatIndex = formatOptions.indexOf("Tables")
        spinnerTournamentFormat.setSelection(defaultFormatIndex)

        // Set default for Privacy to "Private"
        val privacyOptions = resources.getStringArray(R.array.privacy_options).toList()
        val defaultPrivacyIndex = privacyOptions.indexOf("Private")
        spinnerPrivacy.setSelection(defaultPrivacyIndex)
    }

    private fun showTeamNameFields(numTeams: Int) {
        layoutTeamNames.removeAllViews()
        layoutTeamNames.visibility = View.VISIBLE
        for (i in 1..numTeams) {
            val editText = EditText(requireContext())
            editText.hint = "Team $i"
            editText.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutTeamNames.addView(editText)
        }
    }

    private fun handleSubmit() {
        val tournamentName = etTournamentName.text.toString()
        val description = etDescription.text.toString()
        val format = spinnerTournamentFormat.selectedItem.toString()
        val privacy = spinnerPrivacy.selectedItem.toString()
        // val numTeams = spinnerNumTeams.selectedItem.toString().toInt()

        val teamNames = mutableListOf<String>()
        for (i in 0 until layoutTeamNames.childCount) {
            val teamField = layoutTeamNames.getChildAt(i) as EditText
            teamNames.add(teamField.text.toString())
        }

        val newTournament = Tournament(
            name = tournamentName,
            type = privacy,
            format = format,
            description = description,
            teamNames = teamNames
        )
        tournamentsList.add(newTournament)

        Toast.makeText(requireContext(), "Tournament added successfully!", Toast.LENGTH_SHORT).show()
        // Optionally navigate back or refresh the list
    }
}
