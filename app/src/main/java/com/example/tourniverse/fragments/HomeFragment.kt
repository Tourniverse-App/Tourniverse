package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TournamentAdapter
import com.example.tourniverse.models.Tournament
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: TournamentAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var noTournamentsView: TextView
    private val tournaments = mutableListOf<Tournament>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        Log.d("HomeFragment", "onCreateView called")

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerTournaments)
        noTournamentsView = view.findViewById(R.id.noTournamentsView)
        val searchBar = view.findViewById<EditText>(R.id.searchBar)

        // Setup RecyclerView with GridLayoutManager as in XML
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        adapter = TournamentAdapter(tournaments, ::navigateToTournamentDetails)
        recyclerView.adapter = adapter

        // Fetch tournaments
        fetchUserTournaments()

        // Attach search functionality
        searchBar.addTextChangedListener(adapter.getSearchTextWatcher())

        return view
    }

    /**
     * Called when the fragment's activity has been created and the fragment's view hierarchy instantiated.
     * It can be used to do final initialization once these pieces are in place, such as retrieving views or restoring state.
     * It is called after onCreateView, and is called after onStart() and before onResume().
     *
     * @param view The View returned by onCreateView.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        val joinTournamentButton = view.findViewById<Button>(R.id.buttonJoinTournament)

        // Set up join button click listener
        joinTournamentButton.setOnClickListener {
            showJoinTournamentDialog()
        }
    }


    /**
     * Fetches tournaments owned or viewed by the currently logged-in user from Firestore.
     */
    private fun fetchUserTournaments() {
        Log.d("HomeFragment", "Fetching user-specific tournaments from Firestore")

        // Check current user
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("HomeFragment", "No user logged in")
            showNoTournamentsMessage()
            return
        }

        val userId = currentUser.uid
        Log.d("HomeFragment", "Logged-in user ID: $userId")

        // Fetch user's owned tournaments
        db.collection("users").document(userId)
            .collection("tournaments").whereEqualTo("isOwner", true).get()
            .addOnSuccessListener { querySnapshot ->
                tournaments.clear()

                if (querySnapshot.isEmpty) {
                    Log.d("HomeFragment", "No owned tournaments found.")
                    showNoTournamentsMessage()
                    return@addOnSuccessListener
                }

                for (document in querySnapshot.documents) {
                    val tournamentId = document.id

                    // Fetch tournament details from the main tournaments collection
                    db.collection("tournaments").document(tournamentId).get()
                        .addOnSuccessListener { tournamentDocument ->
                            if (tournamentDocument.exists()) {
                                val name = tournamentDocument.getString("name") ?: "Unknown"
                                val privacy = tournamentDocument.getString("privacy") ?: "Private"
                                val description = tournamentDocument.getString("description") ?: ""
                                val teamNames = tournamentDocument.get("teamNames") as? List<String> ?: emptyList()
                                val ownerId = tournamentDocument.getString("ownerId") ?: "Unknown"

                                tournaments.add(
                                    Tournament(
                                        id = tournamentId,
                                        name = name,
                                        type = privacy,
                                        description = description,
                                        teamNames = teamNames,
                                        owner = ownerId,
                                        viewers = emptyList()
                                    )
                                )

                                adapter.filter("") // Show all tournaments initially
                                adapter.notifyDataSetChanged()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("HomeFragment", "Error fetching tournament details: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error fetching owned tournaments: ${e.message}")
                showNoTournamentsMessage()
            }
    }

    /**
     * Displays a message when no tournaments are found.
     */
    private fun showNoTournamentsMessage() {
        recyclerView.visibility = View.GONE
        noTournamentsView.visibility = View.VISIBLE
        Toast.makeText(requireContext(), "No tournaments found.", Toast.LENGTH_SHORT).show()
    }

    /**
     * Navigates to tournament details when a tournament is clicked.
     */
    private fun navigateToTournamentDetails(tournament: Tournament) {
        Log.d("HomeFragment", "Navigating to tournament details for ID: ${tournament.id}")
        Toast.makeText(requireContext(), "Opening tournament: ${tournament.name}", Toast.LENGTH_SHORT).show()

        val bundle = Bundle().apply {
            putString("tournamentId", tournament.id) // Pass the tournamentId
            putString("tournamentName", tournament.name)
            putString("tournamentType", tournament.type)
            putString("tournamentFormat", tournament.format) // Pass the format
            putString("tournamentDescription", tournament.description) // Pass the description
        }
        findNavController().navigate(R.id.action_homeFragment_to_tournamentDetailsFragment, bundle)
    }

    /**
     * Joins a tournament as a viewer.
     *
     * @param tournamentId The ID of the tournament to join.
     */
    private fun joinTournament(tournamentId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: run {
            Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val tournamentRef = db.collection("tournaments").document(tournamentId)
        val userRef = db.collection("users").document(userId)

        tournamentRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Toast.makeText(context, "Tournament not found!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val viewers = document.get("viewers") as? List<String> ?: emptyList()
                val ownerId = document.getString("ownerId") ?: ""

                if (userId == ownerId) {
                    Toast.makeText(context, "You are the owner of this tournament!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                if (viewers.contains(userId)) {
                    Toast.makeText(context, "You are already a viewer of this tournament!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Add user to the tournament viewers and increment the member count
                tournamentRef.update(
                    "viewers", FieldValue.arrayUnion(userId),
                    "memberCount", FieldValue.increment(1)
                ).addOnSuccessListener {
                    // Add tournament to user's tournaments subcollection with booleans
                    val tournamentData = mapOf(
                        "isOwner" to false,
                        "push" to true,
                        "ChatMessages" to true,
                        "Comments" to true,
                        "Likes" to true,
                        "Dnd" to false
                    )
                    userRef.collection("tournaments").document(tournamentId).set(tournamentData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Successfully joined the tournament!", Toast.LENGTH_SHORT).show()
                            refreshFragment() // Refresh the fragment after joining
                        }
                        .addOnFailureListener { e ->
                            Log.e("JoinTournament", "Failed to update user tournaments: ${e.message}")
                            Toast.makeText(context, "Error joining tournament.", Toast.LENGTH_SHORT).show()
                        }
                }.addOnFailureListener { e ->
                    Log.e("JoinTournament", "Failed to add user to tournament viewers: ${e.message}")
                    Toast.makeText(context, "Error joining tournament.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("JoinTournament", "Error fetching tournament: ${e.message}")
                Toast.makeText(context, "Error joining tournament.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Shows a dialog to enter the tournament code.
     */
    private fun showJoinTournamentDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_join_tournament, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Join Tournament")
            .setView(dialogView)
            .setPositiveButton("Join") { _, _ ->
                val codeInput = dialogView.findViewById<EditText>(R.id.tournamentCodeInput)
                val tournamentCode = codeInput.text.toString().trim()

                if (tournamentCode.isNotEmpty()) {
                    joinTournament(tournamentCode) // Call your join logic
                } else {
                    Toast.makeText(context, "Please enter a valid tournament code.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun refreshFragment() {
        findNavController().run {
            popBackStack() // Removes the current fragment from the stack
            navigate(R.id.nav_home) // Navigates back to the same fragment using the correct ID
        }
    }
}
