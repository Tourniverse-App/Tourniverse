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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AddTournamentFragment : Fragment() {

    private lateinit var etTournamentName: EditText
    private lateinit var spinnerTournamentType: Spinner
    private lateinit var spinnerNumTeams: Spinner
    private lateinit var layoutTeamNames: LinearLayout
    private lateinit var spinnerPrivacy: Spinner
    private lateinit var etDescription: EditText
    private lateinit var btnSubmitTournament: Button

    private var currentType: String = "Tables"
    private val db = FirebaseFirestore.getInstance()

    /**
     * Inflates the layout for this fragment and initializes the views.
     * Also sets up the spinners for tournament type, number of teams, and privacy.
     * Finally, sets up the submit button to handle form submission.
     *
     * @param inflater The layout inflater object that can be used to inflate any views in the fragment.
     * @param container The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState The previously saved state of the fragment.
     * @return The root view of the fragment.
     */
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

    /**
     * Sets up the spinner for selecting the tournament type (Tables or Knockout).
     * Also sets up the spinner for selecting the number of teams based on the selected type.
     *
     * The tournament type spinner is initialized with two options: Tables and Knockout.
     * The number of teams spinner is initialized with a range of 2 to 32 for Tables type,
     * and with fixed values of 4, 8, 16, and 32 for Knockout type.
     *
     * The team names fields are displayed based on the selected number of teams.
     *
     * The tournament type spinner's selection listener updates the current type and sets up the number of teams spinner.
     * The number of teams spinner's selection listener shows the team name fields based on the selected number of teams.
     *
     * @see setupNumberOfTeamsSpinner
     * @see showTeamNameFields
     * @see currentType
     * @see spinnerTournamentType
     * @see spinnerNumTeams
     */
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
        val teamOptions = if (currentType == "Tables") (2..32).toList() else listOf(4, 8, 16, 32)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, teamOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNumTeams.adapter = adapter

        spinnerNumTeams.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                showTeamNameFields(teamOptions[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDefaultPrivacySpinner() {
        val privacyOptions = listOf("Public", "Private")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, privacyOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrivacy.adapter = adapter
        spinnerPrivacy.setSelection(1)
    }

    private fun showTeamNameFields(numTeams: Int) {
        layoutTeamNames.removeAllViews()
        for (i in 1..numTeams) {
            val editText = EditText(requireContext()).apply {
                hint = "Team $i"
                isSingleLine = true
                inputType = android.view.inputmethod.EditorInfo.TYPE_CLASS_TEXT
            }
            layoutTeamNames.addView(editText)
        }
    }

    /**
     * Handles the form submission by validating the input fields and creating a new tournament.
     *
     * The tournament name and description fields are validated to ensure they are not empty.
     * The team names fields are validated to ensure they are not empty.
     * The user ID is retrieved from Firebase Auth to associate the tournament with the owner.
     *
     * The tournament data is submitted to Firestore using the FirebaseHelper class.
     * The user's ownedTournaments field is updated with the new tournament ID.
     *
     * If the tournament is created successfully, the user is navigated back to the home screen.
     * If there is an error creating the tournament, an error message is displayed and the submit button is enabled.
     *
     * @param tournamentName The name of the tournament.
     * @param description The description of the tournament.
     * @param privacy The privacy setting of the tournament (Public or Private).
     * @param teamNames The list of team names participating in the tournament.
     * @param ownerId The ID of the user who owns the tournament.
     */
    private fun handleSubmit() {
        val tournamentName = etTournamentName.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val privacy = spinnerPrivacy.selectedItem.toString()

        if (tournamentName.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            Log.e("AddTournamentFragment", "Empty fields: Tournament name or description")
            return
        }

        val teamNames = mutableListOf<String>()
        for (i in 0 until layoutTeamNames.childCount) {
            val teamField = layoutTeamNames.getChildAt(i) as EditText
            val teamName = teamField.text.toString().trim()
            if (teamName.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all the team names", Toast.LENGTH_SHORT).show()
                Log.e("AddTournamentFragment", "Empty team name at position $i")
                return
            }
            teamNames.add(teamName)
        }

        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (ownerId.isEmpty()) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
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
            format = currentType, // Tables or Knockout
            callback = { success, error ->
                if (success) {
                    Toast.makeText(requireContext(), "Tournament created successfully!", Toast.LENGTH_SHORT).show()
                    db.collection("tournaments").whereEqualTo("name", tournamentName).limit(1).get()
                        .addOnSuccessListener { documents ->
                            for (document in documents) {
                                val id = document.id
                                val bundle = Bundle().apply {
                                    putString("tournamentId", id)
                                    putString("tournamentName", tournamentName)
                                    putString("tournamentType", privacy)
                                    putString("tournamentFormat", currentType)
                                    putString("tournamentDescription", description)
                                }
                                findNavController().navigate(R.id.action_addTournamentFragment_to_tournamentDetailsFragment, bundle)
                            }
                        }
                        .addOnFailureListener {
                            Log.e("AddTournamentFragment", "Failed to fetch newly created tournament ID")
                        }
                } else {
                    Log.e("AddTournamentFragment", "Error creating tournament: $error")
                    Toast.makeText(requireContext(), "Failed to create tournament: $error", Toast.LENGTH_SHORT).show()
                    btnSubmitTournament.isEnabled = true
                }
            }
        )
    }
}
