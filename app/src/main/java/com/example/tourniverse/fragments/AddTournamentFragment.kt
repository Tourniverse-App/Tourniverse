package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tourniverse.R
import com.example.tourniverse.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_tournament, container, false)

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
            Log.e("AddTournamentFragment", "Empty fields: Tournament name or description")
            return
        }

        val teamNames = (0 until layoutTeamNames.childCount).mapNotNull { index ->
            val teamField = layoutTeamNames.getChildAt(index) as? EditText
            teamField?.text?.toString()?.trim().takeIf { !it.isNullOrEmpty() } ?: run {
                showToast("Please fill all the team names")
                Log.e("AddTournamentFragment", "Empty team name at position $index")
                return
            }
        }

        val ownerId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (ownerId.isEmpty()) {
            showToast("User not authenticated")
            Log.e("AddTournamentFragment", "User not authenticated")
            return
        }

        btnSubmitTournament.isEnabled = false
        Log.d("AddTournamentFragment", "Submitting tournament: $tournamentName")

        FirebaseHelper.addTournament(
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
                Log.e("AddTournamentFragment", "Error creating tournament: $error")
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
        db.collection("tournaments").whereEqualTo("name", tournamentName).limit(1).get()
            .addOnSuccessListener { documents ->
                documents.firstOrNull()?.let { document ->
                    val bundle = Bundle().apply {
                        putString("tournamentId", document.id)
                        putString("tournamentName", tournamentName)
                        putString("tournamentType", privacy)
                        putString("tournamentFormat", currentType)
                        putString("tournamentDescription", description)
                    }
                    findNavController().navigate(
                        R.id.action_addTournamentFragment_to_tournamentDetailsFragment,
                        bundle
                    )
                }
            }
            .addOnFailureListener {
                Log.e("AddTournamentFragment", "Failed to fetch newly created tournament ID")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
