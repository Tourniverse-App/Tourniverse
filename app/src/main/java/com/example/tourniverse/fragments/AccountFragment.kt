package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.tourniverse.R
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AccountFragment : Fragment() {

    // Firebase Auth instance
    private val auth = Firebase.auth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        // Profile Information
        val nameEditText: EditText = view.findViewById(R.id.edit_name)
        val emailEditText: EditText = view.findViewById(R.id.edit_email)
        val phoneEditText: EditText = view.findViewById(R.id.edit_phone)
        val profileImageView: ImageView = view.findViewById(R.id.profile_picture)
        val editPhotoText: TextView = view.findViewById(R.id.edit_photo)
        val saveButton: Button = view.findViewById(R.id.save_button)

        // Password Management
        val resetPasswordButton: Button = view.findViewById(R.id.reset_password)

        // Deactivation/Deletion
        val deleteAccountButton: Button = view.findViewById(R.id.delete_account)

        // Load current user info
        val currentUser = auth.currentUser
        currentUser?.let {
            nameEditText.setText(it.displayName ?: "")
            emailEditText.setText(it.email ?: "")
            // Firebase doesn't store phone numbers directly for most providers
        }

        // Edit Photo Click Listener
        editPhotoText.setOnClickListener {
            Toast.makeText(context, "Edit Photo clicked", Toast.LENGTH_SHORT).show()
            // TODO: Add logic for opening an image picker or photo editor
        }

        // Save Button Click Listener
        saveButton.setOnClickListener {
            val newName = nameEditText.text.toString().trim()
            val newEmail = emailEditText.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentUser?.let { user ->
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    // Add logic to set the profile picture URL if available
                    .build()

                user.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("AccountFragment", "User profile updated.")
                            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("AccountFragment", "Error updating profile", task.exception)
                            Toast.makeText(context, "Failed to update profile: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                // Update email
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
                    currentUser?.delete()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Failed to delete account: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
                .show()
        }

        return view
    }
}
