package com.example.tourniverse.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tourniverse.models.Tournament
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _tournaments = MutableLiveData<List<Tournament>>()
    val tournaments: LiveData<List<Tournament>> get() = _tournaments

    fun fetchUserTournaments(onResult: (List<Tournament>) -> Unit, onError: (String) -> Unit) {
        Log.d("HomeViewModel", "Fetching user-specific tournaments from Firestore")

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("HomeViewModel", "No user logged in")
            onError("No user logged in")
            return
        }

        val userId = currentUser.uid
        Log.d("HomeViewModel", "Logged-in user ID: $userId")

        db.collection("users").document(userId)
            .collection("tournaments").get()
            .addOnSuccessListener { querySnapshot ->
                val tournamentList = mutableListOf<Tournament>()

                if (querySnapshot.isEmpty) {
                    Log.d("HomeViewModel", "No owned tournaments found.")
                    onError("No tournaments found")
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
                                val ownerId = tournamentDocument.getString("ownerId") ?: "Unknown"

                                tournamentList.add(
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

                                onResult(tournamentList)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("HomeViewModel", "Error fetching tournament details: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeViewModel", "Error fetching owned tournaments: ${e.message}")
                onError("Error fetching tournaments: ${e.message}")
            }
    }

    fun joinTournament(tournamentId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return onError("User not logged in!")

        val tournamentRef = db.collection("tournaments").document(tournamentId)
        val userRef = db.collection("users").document(userId)

        tournamentRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onError("Tournament not found!")
                    return@addOnSuccessListener
                }

                val viewers = document.get("viewers") as? List<String> ?: emptyList()
                val ownerId = document.getString("ownerId") ?: ""

                if (userId == ownerId) {
                    onError("You are the owner of this tournament!")
                    return@addOnSuccessListener
                }

                if (viewers.contains(userId)) {
                    onError("You are already a viewer of this tournament!")
                    return@addOnSuccessListener
                }

                tournamentRef.update(
                    "viewers", FieldValue.arrayUnion(userId),
                    "memberCount", FieldValue.increment(1)
                ).addOnSuccessListener {
                    val tournamentData = mapOf(
                        "isOwner" to false,
                        "Push" to true,
                        "Scores" to true,
                        "ChatMessages" to true,
                        "Comments" to true,
                        "Likes" to true
                    )
                    userRef.collection("tournaments").document(tournamentId).set(tournamentData)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onError("Error joining tournament: ${e.message}") }
                }.addOnFailureListener { e ->
                    onError("Error joining tournament: ${e.message}")
                }
            }
            .addOnFailureListener { e -> onError("Error fetching tournament: ${e.message}") }
    }
}
