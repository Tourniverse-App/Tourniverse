package com.example.tourniverse.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TournamentAdapter
import com.example.tourniverse.models.Tournament
import com.example.tourniverse.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth

class UserFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TournamentAdapter
    private lateinit var userNameTextView: TextView
    private lateinit var userBioTextView: TextView
    private lateinit var profileImageView: ImageView
    private val ownedTournaments = mutableListOf<Tournament>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        // Initialize Views
        userNameTextView = view.findViewById(R.id.tvUserName)
        userBioTextView = view.findViewById(R.id.tvUserBio)
        profileImageView = view.findViewById(R.id.ivProfilePic)
        recyclerView = view.findViewById(R.id.recyclerUserTournaments)

        // Set RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = TournamentAdapter(ownedTournaments) { tournament ->
            navigateToTournamentDetails(tournament)
        }
        recyclerView.adapter = adapter

        // Fetch User Details
        fetchUserProfile()

        // Fetch Owned Tournaments
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

                // Set name and bio
                userNameTextView.text = userName
                userBioTextView.text = userBio

                // Set profile image
                if (!profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.ic_user) // Default placeholder
                        .error(R.drawable.ic_user)       // Fallback if image fails
                        .into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.ic_user) // Default image
                }
            }
        }
    }

    /**
     * Fetches tournaments owned by the user.
     */
    private fun fetchOwnedTournaments() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseHelper.getUserTournaments { tournamentsList ->
            ownedTournaments.clear()

            for (data in tournamentsList) {
                val ownerId = data["ownerId"] as? String
                if (ownerId == currentUserId) {
                    val name = data["name"] as? String ?: "Unknown"
                    val privacy = data["privacy"] as? String ?: "Private"
                    val description = data["description"] as? String ?: ""
                    val teamNames = data["teamNames"] as? List<String> ?: emptyList()
                    val viewers = data["viewers"] as? List<String> ?: emptyList()

                    val tournament = Tournament(
                        name = name,
                        type = privacy,
                        format = null,
                        description = description,
                        teamNames = teamNames,
                        owner = ownerId,
                        viewers = viewers
                    )
                    ownedTournaments.add(tournament)
                }
            }

            adapter.notifyDataSetChanged()
        }
    }

    /**
     * Navigates to tournament details when a tournament is clicked.
     */
    private fun navigateToTournamentDetails(tournament: Tournament) {
        val bundle = Bundle().apply {
            putString("tournamentName", tournament.name)
            putString("tournamentType", tournament.type)
        }
        findNavController().navigate(R.id.action_userFragment_to_tournamentDetailsFragment, bundle)
    }
}
