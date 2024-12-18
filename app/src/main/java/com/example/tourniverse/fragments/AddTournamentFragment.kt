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

        val tournamentData = hashMapOf(
            "name" to tournamentName,
            "description" to description,
            "privacy" to privacy,
            "teamCount" to teamNames.size,
            "ownerId" to ownerId,
            "teamNames" to teamNames,
            "createdAt" to System.currentTimeMillis(),
            "type" to currentType
        )

        db.collection("tournaments").add(tournamentData)
            .addOnSuccessListener { documentReference ->
                val tournamentId = documentReference.id
                Log.d("AddTournamentFragment", "Tournament created with ID: $tournamentId")
                generateMatches(tournamentId, teamNames, currentType)
                updateUserOwnedTournaments(ownerId, tournamentId)
                Toast.makeText(requireContext(), "Tournament created!", Toast.LENGTH_SHORT).show()
                val bundle = Bundle().apply { putString("tournamentId", tournamentId) }
                findNavController().navigate(R.id.action_addTournamentFragment_to_tournamentDetailsFragment, bundle)
            }
            .addOnFailureListener { e ->
                Log.e("AddTournamentFragment", "Error creating tournament: ${e.message}")
                Toast.makeText(requireContext(), "Error creating tournament: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSubmitTournament.isEnabled = true
            }
    }

    private fun updateUserOwnedTournaments(userId: String, tournamentId: String) {
        val userDocRef = db.collection("users").document(userId)
        userDocRef.update("ownedTournaments", FieldValue.arrayUnion(tournamentId))
            .addOnSuccessListener {
                Log.d("AddTournamentFragment", "Tournament ID added to user's owned tournaments")
            }
            .addOnFailureListener { e ->
                Log.e("AddTournamentFragment", "Error updating user's owned tournaments: ${e.message}")
            }
    }

    private fun generateMatches(tournamentId: String, teamNames: List<String>, type: String) {
        val matches = mutableListOf<HashMap<String, Any>>()

        if (type == "Tables") {
            for (i in teamNames.indices) {
                for (j in i + 1 until teamNames.size) {
                    matches.add(
                        hashMapOf(
                            "teamA" to teamNames[i],
                            "teamB" to teamNames[j],
                            "scoreA" to 0,
                            "scoreB" to 0
                        )
                    )
                }
            }
        } else if (type == "Knockout") {
            for (i in 0 until teamNames.size -1 step 2) {
                matches.add(
                    hashMapOf(
                        "teamA" to teamNames[i],
                        "teamB" to teamNames[i+1],
                        "scoreA" to 0,
                        "scoreB" to 0
                    )
                )
            }
        }

        db.collection("tournaments").document(tournamentId)
            .collection("matches").add(mapOf("matches" to matches))
            .addOnSuccessListener {
                Log.d("AddTournamentFragment", "Matches generated successfully for tournament ID: $tournamentId")
            }
            .addOnFailureListener { e ->
                Log.e("AddTournamentFragment", "Error generating matches: ${e.message}")
                Toast.makeText(requireContext(), "Error generating matches: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
