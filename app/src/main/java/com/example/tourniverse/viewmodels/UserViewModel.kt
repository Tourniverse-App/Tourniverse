package com.example.tourniverse.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tourniverse.models.Tournament
import com.example.tourniverse.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _userProfile = MutableLiveData<Map<String, String>>()
    val userProfile: LiveData<Map<String, String>> get() = _userProfile

    private val _ownedTournaments = MutableLiveData<List<Tournament>>()
    val ownedTournaments: LiveData<List<Tournament>> get() = _ownedTournaments

    /**
     * Fetches and updates the user's profile data.
     */
    fun fetchUserProfile(onError: (String) -> Unit) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseHelper.getUserDocument(currentUserId) { userData ->
            userData?.let { data ->
                val userName = data["username"] as? String ?: "User"
                val userBio = data["bio"] as? String ?: "No bio available"

                _userProfile.postValue(
                    mapOf(
                        "username" to userName,
                        "bio" to userBio
                    )
                )
            } ?: run {
                onError("Failed to load user profile.")
            }
        }
    }

    /**
     * Fetches and updates the list of tournaments owned by the user.
     */
    fun fetchOwnedTournaments(onError: (String) -> Unit) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(currentUserId)
            .collection("tournaments").whereEqualTo("isOwner", true).get()
            .addOnSuccessListener { querySnapshot ->
                val tournaments = mutableListOf<Tournament>()

                if (querySnapshot.isEmpty) {
                    Log.d("UserViewModel", "No owned tournaments found.")
                    _ownedTournaments.postValue(tournaments)
                    return@addOnSuccessListener
                }

                for (document in querySnapshot.documents) {
                    val tournamentId = document.id

                    db.collection("tournaments").document(tournamentId).get()
                        .addOnSuccessListener { tournamentDocument ->
                            if (tournamentDocument.exists()) {
                                val name = tournamentDocument.getString("name") ?: "Unknown"
                                val privacy = tournamentDocument.getString("privacy") ?: "Private"
                                val description = tournamentDocument.getString("description") ?: ""
                                val teamNames = tournamentDocument.get("teamNames") as? List<String> ?: emptyList()
                                val viewers = tournamentDocument.get("viewers") as? List<String> ?: emptyList()
                                val format = tournamentDocument.getString("type") ?: ""

                                tournaments.add(
                                    Tournament(
                                        id = tournamentId,
                                        name = name,
                                        type = privacy,
                                        description = description,
                                        teamNames = teamNames,
                                        owner = currentUserId,
                                        viewers = viewers,
                                        format = format
                                    )
                                )

                                _ownedTournaments.postValue(tournaments)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("UserViewModel", "Error fetching tournament details: ${e.message}")
                            onError("Error fetching tournament details.")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Error fetching owned tournaments: ${e.message}")
                onError("Failed to load owned tournaments.")
            }
    }
}
