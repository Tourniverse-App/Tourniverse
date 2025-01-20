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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.TournamentAdapter
import com.example.tourniverse.models.Tournament
import com.example.tourniverse.viewmodels.UserViewModel

class UserFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TournamentAdapter
    private lateinit var searchBar: EditText
    private lateinit var userNameTextView: TextView
    private lateinit var userBioTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var viewModel: UserViewModel

    private val ownedTournaments = mutableListOf<Tournament>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(UserViewModel::class.java)

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

        // Observe data from ViewModel
        observeViewModel()

        // Fetch user profile and tournaments
        fetchUserProfile()
        fetchOwnedTournaments()

        return view
    }

    private fun fetchUserProfile() {
        viewModel.fetchUserProfile { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchOwnedTournaments() {
        viewModel.fetchOwnedTournaments { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            userNameTextView.text = profile["username"]
            userBioTextView.text = profile["bio"]

            // Decode Base64 profile photo or fallback to placeholder
            val profilePhoto = profile["profilePhoto"]
            if (!profilePhoto.isNullOrEmpty()) {
                try {
                    // Decode Base64 string into a Bitmap
                    val decodedBytes = android.util.Base64.decode(profilePhoto, android.util.Base64.DEFAULT)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                    // Set the decoded Bitmap to the ImageView
                    profileImageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    // If decoding fails, log the error and set a default placeholder
                    profileImageView.setImageResource(R.drawable.ic_user)
                    Log.e("UserFragment", "Error decoding profile photo: ${e.message}")
                }
            } else {
                // If profile photo is empty, use a default placeholder
                profileImageView.setImageResource(R.drawable.ic_user)
            }
        }

        viewModel.ownedTournaments.observe(viewLifecycleOwner) { tournaments ->
            ownedTournaments.clear()
            ownedTournaments.addAll(tournaments)
            adapter.filter("")
            adapter.notifyDataSetChanged()
        }
    }

    private fun navigateToTournamentDetails(tournament: Tournament) {
        val bundle = Bundle().apply {
            putString("tournamentId", tournament.id)
            putString("tournamentName", tournament.name)
            putString("tournamentType", tournament.type)
            putString("tournamentFormat", tournament.format)
            putString("tournamentDescription", tournament.description)
        }
        findNavController().navigate(R.id.action_userFragment_to_tournamentDetailsFragment, bundle)
    }
}
