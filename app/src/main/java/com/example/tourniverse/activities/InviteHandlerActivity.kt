package com.example.tourniverse

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tourniverse.activities.LoginActivity
import com.example.tourniverse.activities.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class InviteHandlerActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val userId: String? by lazy { FirebaseAuth.getInstance().currentUser?.uid }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tournamentId = intent?.data?.getQueryParameter("tournamentId") // Get ID from URL

        if (userId == null) {
            Toast.makeText(this, "You need to log in first!", Toast.LENGTH_SHORT).show()
            redirectToLogin()
            return
        }

        if (tournamentId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid invite link.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        checkAndAddViewer(tournamentId)
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
