package com.example.tourniverse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tourniverse.activities.LoginActivity
import com.example.tourniverse.activities.MainActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.dynamicLinks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class InviteHandlerActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val userId: String? by lazy { FirebaseAuth.getInstance().currentUser?.uid }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseDynamicLinks.getInstance()
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                val deepLink: Uri? = pendingDynamicLinkData?.link
                if (deepLink != null) {
                    val tournamentId = deepLink.getQueryParameter("tournamentId")
                    if (tournamentId != null) {
                        handleInvite(tournamentId)
                    } else {
                        Toast.makeText(this, "Invalid invite link!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to process invite link.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleInvite(tournamentId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val ownerId = document.getString("ownerId")
                    val viewers = document.get("viewers") as? List<String> ?: emptyList()

                    if (ownerId == userId) {
                        Toast.makeText(this, "You are already the owner.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else if (viewers.contains(userId)) {
                        Toast.makeText(this, "You are already a viewer.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        // Add user to viewers and viewedTournaments
                        db.collection("tournaments").document(tournamentId)
                            .update("viewers", FieldValue.arrayUnion(userId))
                        db.collection("users").document(userId)
                            .update("viewedTournaments", FieldValue.arrayUnion(tournamentId))
                            .addOnSuccessListener {
                                Toast.makeText(this, "You have joined the tournament!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to join tournament.", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Tournament not found!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching tournament details.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }


    private fun checkAndAddViewer(tournamentId: String) {
        db.collection("tournaments").document(tournamentId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Toast.makeText(this, "Tournament does not exist!", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val ownerId = document.getString("ownerId") ?: ""
                val viewers = document.get("viewers") as? List<String> ?: emptyList()

                // Check if the user is already the owner or a viewer
                if (userId == ownerId || viewers.contains(userId)) {
                    Toast.makeText(this, "You are already part of this tournament!", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Add user as viewer
                db.collection("tournaments").document(tournamentId)
                    .update("viewers", FieldValue.arrayUnion(userId))
                    .addOnSuccessListener {
                        db.collection("users").document(userId!!)
                            .update("viewedTournaments", FieldValue.arrayUnion(tournamentId))
                            .addOnSuccessListener {
                                Toast.makeText(this, "Added as a viewer!", Toast.LENGTH_SHORT).show()
                                redirectToTournament(tournamentId)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to update user: ${e.message}", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to add viewer: ${e.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching tournament: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun redirectToTournament(tournamentId: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("tournamentId", tournamentId) // Pass the ID to the main activity
        startActivity(intent)
        finish()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
