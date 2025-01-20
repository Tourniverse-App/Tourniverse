package com.example.tourniverse.viewmodels

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tourniverse.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.android.gms.tasks.Tasks
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.core.content.ContentProviderCompat.requireContext
import java.io.ByteArrayOutputStream

class AccountViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    fun fetchUserDetails(userId: String, onSuccess: (String, String, String) -> Unit) {
        Log.d("AccountViewModel", "Fetching user details for userId: $userId")
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username").orEmpty()
                    val email = document.getString("email").orEmpty()
                    val bio = document.getString("bio").orEmpty()
                    Log.d("AccountViewModel", "Fetched user details: username=$username, bio=$bio")
                    onSuccess(username, email, bio)
                } else {
                    Log.w("AccountViewModel", "User document does not exist for userId: $userId")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AccountViewModel", "Error fetching user document for userId: $userId", exception)
            }
    }

    fun saveProfile(
        user: FirebaseUser?,
        username: String,
        email: String,
        bio: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (username.isEmpty()) {
            Log.e("AccountViewModel", "Username cannot be empty.")
            onError("Username cannot be empty.")
            return
        }

        user?.let {
            Log.d("AccountViewModel", "Saving profile for userId: ${it.uid}")
            db.collection("users").document(it.uid)
                .update("username", username, "bio", bio)
                .addOnSuccessListener {
//                    Log.d("AccountViewModel", "Profile updated successfully for userId: ${it.uid}")
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    Log.e("AccountViewModel", "Error updating profile for userId: ${it.uid}", exception)
                    onError(exception.message.orEmpty())
                }

            if (email != it.email) {
                it.updateEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AccountViewModel", "Email updated successfully for userId: ${it.uid}")
                        onSuccess()
                    } else {
                        Log.e("AccountViewModel", "Error updating email for userId: ${it.uid}", task.exception)
                        onError(task.exception?.message.orEmpty())
                    }
                }
            }
        } ?: Log.e("AccountViewModel", "User is null, cannot save profile.")
    }

    fun sendPasswordReset(email: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isNullOrEmpty()) {
            Log.e("AccountViewModel", "No email associated with this account.")
            onError("No email associated with this account.")
            return
        }

        Log.d("AccountViewModel", "Sending password reset email to: $email")
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AccountViewModel", "Password reset email sent to: $email")
                    onSuccess()
                } else {
                    Log.e("AccountViewModel", "Error sending password reset email to: $email", task.exception)
                    onError(task.exception?.message.orEmpty())
                }
            }
    }

    fun confirmAndDeleteAccount(
        context: Context,
        user: FirebaseUser?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Show a confirmation dialog to the user
        AlertDialog.Builder(context)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                // Proceed with account deletion
                deleteAccount(user, onSuccess, onError)
            }
            .setNegativeButton("No") { dialog, _ ->
                // Cancel the deletion
                dialog.dismiss()
                Log.d("AccountViewModel", "Account deletion canceled by the user.")
            }
            .show()
    }

    fun deleteAccount(
        user: FirebaseUser?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        user?.let { currentUser ->
            Log.d("AccountViewModel", "Deleting account for userId: ${currentUser.uid}")
            db.collection("tournaments")
                .whereArrayContains("viewers", currentUser.uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    Log.d("AccountViewModel", "Removing user from viewers for userId: ${currentUser.uid}")
                    val tasks = querySnapshot.documents.map { document ->
                        db.collection("tournaments").document(document.id)
                            .update("viewers", FieldValue.arrayRemove(currentUser.uid))
                    }

                    Tasks.whenAll(tasks).addOnSuccessListener {
                        db.collection("tournaments")
                            .whereEqualTo("ownerId", currentUser.uid)
                            .get()
                            .addOnSuccessListener { ownerQuerySnapshot ->
                                Log.d("AccountViewModel", "Deleting user's tournaments for userId: ${currentUser.uid}")
                                ownerQuerySnapshot.documents.forEach { document ->
                                    FirebaseHelper.deleteSubcollectionsAndDocument(document.id)
                                }

                                FirebaseHelper.deleteUserData(currentUser.uid) {
                                    currentUser.delete().addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Log.d("AccountViewModel", "Account deleted successfully for userId: ${currentUser.uid}")
                                            Firebase.auth.signOut() // Sign out the user
                                            onSuccess() // Notify success
                                        } else {
                                            Log.e("AccountViewModel", "Error deleting account for userId: ${currentUser.uid}", task.exception)
                                            onError(task.exception?.message.orEmpty())
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("AccountViewModel", "Error fetching user's tournaments: ${exception.message}", exception)
                                onError(exception.message.orEmpty())
                            }
                    }.addOnFailureListener { exception ->
                        Log.e("AccountViewModel", "Error removing user from tournaments: ${exception.message}", exception)
                        onError(exception.message.orEmpty())
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("AccountViewModel", "Error fetching tournaments where user is a viewer: ${exception.message}", exception)
                    onError(exception.message.orEmpty())
                }
        } ?: run {
            Log.e("AccountViewModel", "User is null, cannot delete account.")
            onError("User is not logged in.")
        }
    }

    fun uploadProfilePhoto(userId: String, base64String: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        Log.d("AccountViewModel", "Uploading profile photo for userId: $userId")
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        userDocRef.update("profilePhoto", base64String)
            .addOnSuccessListener {
                Log.d("AccountViewModel", "Profile photo uploaded successfully for userId: $userId")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("AccountViewModel", "Error uploading profile photo for userId: $userId", exception)
                onError(exception.message ?: "Unknown error occurred")
            }
    }

    fun fetchUserPhoto(userId: String, onResult: (String?) -> Unit) {
        Log.d("AccountViewModel", "Fetching profile photo for userId: $userId")
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        userDocRef.get()
            .addOnSuccessListener { document ->
                val base64String = document.getString("profilePhoto")
                if (base64String != null) {
                    Log.d("AccountViewModel", "Profile photo fetched successfully for userId: $userId")
                } else {
                    Log.w("AccountViewModel", "No profile photo found for userId: $userId")
                }
                onResult(base64String)
            }
            .addOnFailureListener { exception ->
                Log.e("AccountViewModel", "Error fetching profile photo for userId: $userId", exception)
                onResult(null)
            }
    }
}
