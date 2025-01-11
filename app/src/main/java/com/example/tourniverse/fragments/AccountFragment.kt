package com.example.tourniverse.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.tourniverse.R
import com.example.tourniverse.activities.LoginActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class AccountFragment : Fragment() {

    // Firebase Auth and Firestore instances
    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        // Profile Information
        val usernameEditText: EditText = view.findViewById(R.id.edit_name)
        val emailEditText: EditText = view.findViewById(R.id.edit_email)
        val bioEditText: EditText = view.findViewById(R.id.edit_bio) // New bio field
        val saveButton: Button = view.findViewById(R.id.save_button)

        // Password Management
        val resetPasswordButton: Button = view.findViewById(R.id.reset_password)

        // Deactivation/Deletion
        val deleteAccountButton: Button = view.findViewById(R.id.delete_account)

        // Load current user info
        val currentUser = auth.currentUser
        if (currentUser != null) {
            emailEditText.setText(currentUser.email ?: "")
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username") ?: ""
                        val bio = document.getString("bio") ?: ""
                        usernameEditText.setText(username)
                        bioEditText.setText(bio)
                    } else {
                        Log.w("AccountFragment", "User document does not exist.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AccountFragment", "Error fetching user document: ${e.message}")
                }
        }

        // Save Button Click Listener
        saveButton.setOnClickListener {
            val newUsername = usernameEditText.text.toString().trim()
            val newEmail = emailEditText.text.toString().trim()
            val newBio = bioEditText.text.toString().trim()

            if (newUsername.isEmpty()) {
                Toast.makeText(context, "Username cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentUser?.let { user ->
                // Update Firestore with new username
                db.collection("users").document(user.uid)
                    .update("username", newUsername, "bio", newBio)
                    .addOnSuccessListener {
                        Log.d("AccountFragment", "Username and bio updated in Firestore.")
                        Toast.makeText(context, "Username updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("AccountFragment", "Error updating username: ${e.message}")
                        Toast.makeText(context, "Failed to update username.", Toast.LENGTH_SHORT).show()
                    }

                // Update email in Firebase Auth
                if (newEmail != user.email) {
                    user.updateEmail(newEmail)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("AccountFragment", "User email updated.")
                                Toast.makeText(context, "Email updated successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("AccountFragment", "Error updating email", task.exception)
                                Toast.makeText(context, "Failed to update email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }

        // Reset Password
        resetPasswordButton.setOnClickListener {
            val email = currentUser?.email
            if (email != null) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                context,
                                "Password reset email sent to $email",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Failed to send reset email: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(context, "No email associated with this account.", Toast.LENGTH_SHORT).show()
            }
        }

        // Delete Account
        deleteAccountButton.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    val currentUser = auth.currentUser
                    currentUser?.let { user ->
                        val userEmail = user.email

                        if (userEmail != null) {
                            // Step 1: Remove user from all tournaments
                            db.collection("tournaments")
                                .whereArrayContains("viewers", user.uid)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    for (document in querySnapshot.documents) {
                                        val tournamentId = document.id
                                        db.collection("tournaments").document(tournamentId)
                                            .update("viewers", FieldValue.arrayRemove(user.uid))
                                            .addOnSuccessListener {
                                                Log.d("AccountFragment", "User removed from tournament: $tournamentId")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("AccountFragment", "Error removing user from tournament: ${e.message}")
                                            }
                                    }

                                    // Step 2: Delete Firestore user document
                                    db.collection("users").document(user.uid)
                                        .delete()
                                        .addOnSuccessListener {
                                            Log.d("AccountFragment", "User document deleted successfully.")

                                            // Step 3: Delete user from Firebase Authentication
                                            user.delete()
                                                .addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        Log.d("AccountFragment", "User account deleted from Firebase Authentication.")
                                                        Toast.makeText(
                                                            context,
                                                            "Account deleted successfully.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()

                                                        // Step 4: Redirect to Login Activity
                                                        val intent = Intent(context, LoginActivity::class.java)
                                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        startActivity(intent)
                                                    } else {
                                                        Log.e("AccountFragment", "Failed to delete user from Authentication: ${task.exception?.message}")
                                                        Toast.makeText(
                                                            context,
                                                            "Failed to delete account: ${task.exception?.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("AccountFragment", "Failed to delete user document: ${e.message}")
                                            Toast.makeText(
                                                context,
                                                "Failed to delete account data: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("AccountFragment", "Failed to query tournaments: ${e.message}")
                                    Toast.makeText(
                                        context,
                                        "Failed to remove user from tournaments: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Toast.makeText(context, "No email associated with this account.", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Toast.makeText(context, "No user is currently signed in.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
                .show()
        }

        return view
    }
}
