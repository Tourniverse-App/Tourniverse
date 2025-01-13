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
import com.example.tourniverse.utils.FirebaseHelper
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

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
                            // Step 0: Sign out the user to prevent auto-login
                            auth.signOut()

                            // Redirect to LoginActivity
                            val intent = Intent(context, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            intent.putExtra("ACCOUNT_DELETION_MESSAGE", "Your account is being deleted.")
                            startActivity(intent)

                            // Perform account deletion tasks in the background

                            // Step 1: Remove user from all tournaments
                            db.collection("tournaments")
                                .whereArrayContains("viewers", user.uid)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    val tasks = mutableListOf<Task<Void>>()

                                    for (document in querySnapshot.documents) {
                                        val tournamentId = document.id
                                        val removeViewerTask = db.collection("tournaments").document(tournamentId)
                                            .update("viewers", FieldValue.arrayRemove(user.uid))
                                        tasks.add(removeViewerTask)
                                    }

                                    Tasks.whenAll(tasks)
                                        .addOnSuccessListener {
                                            // Step 2: Delete tournaments owned by the user
                                            db.collection("tournaments")
                                                .whereEqualTo("ownerId", user.uid)
                                                .get()
                                                .addOnSuccessListener { ownerQuerySnapshot ->
                                                    val deleteTournamentTasks = mutableListOf<Task<Void>>()

                                                    for (document in ownerQuerySnapshot.documents) {
                                                        val tournamentId = document.id
                                                        FirebaseHelper.deleteSubcollectionsAndDocument(tournamentId)
                                                    }

                                                    Tasks.whenAll(deleteTournamentTasks)
                                                        .addOnSuccessListener {
                                                            // Step 3: Delete user data (subcollections and Firestore document)
                                                            FirebaseHelper.deleteUserData(user.uid) {
                                                                // Step 4: Delete user from Firebase Authentication
                                                                user.delete()
                                                                    .addOnCompleteListener { task ->
                                                                        if (task.isSuccessful) {
                                                                            Log.d("AccountFragment", "User account deleted from Firebase Authentication.")
                                                                        } else {
                                                                            Log.e("AccountFragment", "Failed to delete user from Authentication: ${task.exception?.message}")
                                                                        }
                                                                    }
                                                            }
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.e("AccountFragment", "Failed to delete owned tournaments: ${e.message}")
                                                        }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("AccountFragment", "Failed to query owned tournaments: ${e.message}")
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("AccountFragment", "Failed to remove user from tournaments: ${e.message}")
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("AccountFragment", "Failed to query tournaments: ${e.message}")
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

    private fun deleteUserSubcollections(userId: String, onComplete: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("users").document(userId)

        // List of subcollections to delete
        val subcollections = listOf("tournaments", "notifications")

        val batch = db.batch()
        val tasks = mutableListOf<Task<Void>>()

        for (subcollection in subcollections) {
            val task = userDocRef.collection(subcollection).get()
                .continueWithTask { querySnapshot ->
                    for (document in querySnapshot.result.documents) {
                        batch.delete(document.reference)
                    }
                    batch.commit()
                }
            tasks.add(task)
        }

        Tasks.whenAll(tasks)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("AccountFragment", "Failed to delete subcollections: ${e.message}")
            }
    }
}
