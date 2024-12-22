package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TournamentAdapter
import com.example.tourniverse.models.Tournament
import com.example.tourniverse.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class UserFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TournamentAdapter
    private lateinit var searchBar: EditText
    private lateinit var userNameTextView: TextView
    private lateinit var userBioTextView: TextView
    private lateinit var profileImageView: ImageView
    private val ownedTournaments = mutableListOf<Tournament>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        // Initialize views
        userNameTextView = view.findViewById(R.id.tvUserName)
        userBioTextView = view.findViewById(R.id.tvUserBio)
        profileImageView = view.findViewById(R.id.ivProfilePic)
        recyclerView = view.findViewById(R.id.recyclerUserTournaments)
        searchBar = view.findViewById(R.id.searchBar)

        // Set up RecyclerView
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        adapter = TournamentAdapter(ownedTournaments) { tournament ->
            navigateToTournamentDetails(tournament)
        }
        recyclerView.adapter = adapter

        // Attach search functionality
        searchBar.addTextChangedListener(adapter.getSearchTextWatcher())

        // Fetch user details and owned tournaments
        fetchUserProfile()
        fetchOwnedTournaments()

        return view
    }

    /**
     * Fetches and displays user profile data (name, bio, and profile picture).
     */
    private fun fetchUserProfile() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseHelper.getUserDocument(currentUserId) { userData ->
            userData?.let { data ->
                val userName = data["username"] as? String ?: "User"
                val userBio = data["bio"] as? String ?: "No bio available"
                val profileImageUrl = data["image"] as? String

                userNameTextView.text = userName
                userBioTextView.text = userBio

            }
        }
    }

    /**
     * Fetches tournaments owned by the user and updates the adapter.
     */
    private fun fetchOwnedTournaments() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDocument ->
                if (!userDocument.exists()) {
                    Log.d("UserFragment", "User document does not exist.")
                    Toast.makeText(requireContext(), "No Owned Tournaments Yet", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Extract owned tournaments
                val ownedTournamentsIds = userDocument.get("ownedTournaments") as? List<String> ?: emptyList()
                Log.d("UserFragment", "Owned Tournaments IDs: $ownedTournamentsIds")

                if (ownedTournamentsIds.isEmpty()) {
                    Log.d("UserFragment", "No owned tournaments found.")
                    Toast.makeText(requireContext(), "No Owned Tournaments Yet", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Fetch tournament data
                FirebaseFirestore.getInstance().collection("tournaments")
                    .whereIn(FieldPath.documentId(), ownedTournamentsIds)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        ownedTournaments.clear()

                        if (querySnapshot.isEmpty) {
                            Log.d("UserFragment", "No tournaments matched the owned IDs.")
                            Toast.makeText(requireContext(), "No Owned Tournaments Yet", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        for (document in querySnapshot.documents) {
                            Log.d("UserFragment", "Fetched tournament: ${document.data}")

                            val id = document.id
                            val name = document.getString("name") ?: "Unknown"
                            val privacy = document.getString("privacy") ?: "Private"
                            val description = document.getString("description") ?: ""
                            val teamNames = document.get("teamNames") as? List<String> ?: emptyList()
                            val viewers = document.get("viewers") as? List<String> ?: emptyList()
                            val format = document.getString("type") ?: ""

                            ownedTournaments.add(
                                Tournament(
                                    id = id,
                                    name = name,
                                    type = privacy,
                                    description = description,
                                    teamNames = teamNames,
                                    owner = currentUserId,
                                    viewers = viewers,
                                    format = format
                                )
                            )
                        }

                        adapter.filter("") // Show all tournaments initially
                        adapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserFragment", "Error fetching owned tournaments: ${e.message}")
                        Toast.makeText(requireContext(), "Failed to load owned tournaments.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("UserFragment", "Error fetching user data: ${e.message}")
                Toast.makeText(requireContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun navigateToTournamentDetails(tournament: Tournament) {
        val bundle = Bundle().apply {
            putString("tournamentId", tournament.id) // Pass the tournament ID
            putString("tournamentName", tournament.name) // Pass the tournament name
            putString("tournamentType", tournament.type) // Pass the tournament type
            putString("tournamentFormat", tournament.format) // Pass the tournament format
            putString("tournamentDescription", tournament.description) // Pass the description
        }
        findNavController().navigate(R.id.action_userFragment_to_tournamentDetailsFragment, bundle)
    }

}
