package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.tourniverse.R
import com.example.tourniverse.viewmodels.AddTournamentViewModel
import com.google.firebase.auth.FirebaseAuth

class AddTournamentFragment : Fragment() {

    // UI Components
    private lateinit var etTournamentName: EditText
    private lateinit var spinnerTournamentType: Spinner
    private lateinit var spinnerNumTeams: Spinner
    private lateinit var layoutTeamNames: LinearLayout
    private lateinit var spinnerPrivacy: Spinner
    private lateinit var etDescription: EditText
    private lateinit var btnSubmitTournament: Button

    private var currentType: String = "Tables"
    private lateinit var viewModel: AddTournamentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_tournament, container, false)
        viewModel = ViewModelProvider(this).get(AddTournamentViewModel::class.java)

        // Initialize Views
        etTournamentName = view.findViewById(R.id.etTournamentName)
        spinnerTournamentType = view.findViewById(R.id.spinnerTournamentType)
        spinnerNumTeams = view.findViewById(R.id.spinnerNumTeams)
        layoutTeamNames = view.findViewById(R.id.layoutTeamNames)
        spinnerPrivacy = view.findViewById(R.id.spinnerPrivacy)
        etDescription = view.findViewById(R.id.etDescription)
        btnSubmitTournament = view.findViewById(R.id.btnSubmitTournament)

        setupTournamentTypeSpinner()
        setupDefaultPrivacySpinner()

        btnSubmitTournament.setOnClickListener { handleSubmit() }

        return view
    }

    private fun setupTournamentTypeSpinner() {
        val typeOptions = listOf("Tables", "Knockout")
        spinnerTournamentType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            typeOptions
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spinnerTournamentType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentType = typeOptions[position]
                setupNumberOfTeamsSpinner()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupNumberOfTeamsSpinner() {
        val teamOptions = if (currentType == "Tables") (2..32).toList() else listOf(4, 8, 16, 32)
        spinnerNumTeams.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            teamOptions
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spinnerNumTeams.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                showTeamNameFields(teamOptions[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDefaultPrivacySpinner() {
        val privacyOptions = listOf("Public", "Private")
        spinnerPrivacy.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            privacyOptions
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spinnerPrivacy.setSelection(1)
    }

    private fun showTeamNameFields(numTeams: Int) {
        layoutTeamNames.removeAllViews()
        repeat(numTeams) { index ->
            layoutTeamNames.addView(EditText(requireContext()).apply {
                hint = "Team ${index + 1}"
                isSingleLine = true
            })
        }
    }

    private fun handleSubmit() {
        val tournamentName = etTournamentName.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val privacy = spinnerPrivacy.selectedItem.toString()

        if (tournamentName.isEmpty() || description.isEmpty()) {
            showToast("Please fill all fields")
            return
        }

        val teamNames = (0 until layoutTeamNames.childCount).mapNotNull { index ->
            val teamField = layoutTeamNames.getChildAt(index) as? EditText
            teamField?.text?.toString()?.trim().takeIf { !it.isNullOrEmpty() } ?: run {
                showToast("Please fill all the team names")
                return
            }
        }

        val ownerId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (ownerId.isEmpty()) {
            showToast("User not authenticated")
            return
        }

        btnSubmitTournament.isEnabled = false

        viewModel.addTournament(
            name = tournamentName,
            teamCount = teamNames.size,
            description = description,
            privacy = privacy,
            teamNames = teamNames,
            format = currentType
        ) { success, error ->
            if (success) {
                navigateToTournamentDetails(tournamentName, description, privacy)
            } else {
                showToast("Failed to create tournament: $error")
                btnSubmitTournament.isEnabled = true
            }
        }
    }

    private fun navigateToTournamentDetails(
        tournamentName: String,
        description: String,
        privacy: String
    ) {
        viewModel.fetchTournamentId(
            tournamentName,
            onSuccess = { tournamentId ->
                val bundle = Bundle().apply {
                    putString("tournamentId", tournamentId)
                    putString("tournamentName", tournamentName)
                    putString("tournamentType", privacy)
                    putString("tournamentFormat", currentType)
                    putString("tournamentDescription", description)
                }
                findNavController().navigate(
                    R.id.action_addTournamentFragment_to_tournamentDetailsFragment,
                    bundle
                )
            },
            onFailure = {
                showToast(it)
                btnSubmitTournament.isEnabled = true
            }
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
