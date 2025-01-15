package com.example.tourniverse.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.tourniverse.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.android.gms.tasks.Tasks

class AccountViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    fun fetchUserDetails(userId: String, onSuccess: (String, String) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onSuccess(
                        document.getString("username").orEmpty(),
                        document.getString("bio").orEmpty()
                    )
                } else {
                    Log.w("AccountViewModel", "User document does not exist.")
                }
            }
            .addOnFailureListener { Log.e("AccountViewModel", "Error fetching user document: ${it.message}") }
    }

    fun saveProfile(user: FirebaseUser?, username: String, email: String, bio: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (username.isEmpty()) {
            onError("Username cannot be empty.")
            return
        }

        user?.let {
            db.collection("users").document(it.uid)
                .update("username", username, "bio", bio)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it.message.orEmpty()) }

            if (email != it.email) {
                it.updateEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) onSuccess()
                    else onError(task.exception?.message.orEmpty())
                }
            }
        }
    }

    fun sendPasswordReset(email: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isNullOrEmpty()) {
            onError("No email associated with this account.")
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onSuccess()
                else onError(task.exception?.message.orEmpty())
            }
    }

    fun deleteAccount(user: FirebaseUser?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        user?.let { currentUser ->
            db.collection("tournaments")
                .whereArrayContains("viewers", currentUser.uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val tasks = querySnapshot.documents.map { document ->
                        db.collection("tournaments").document(document.id)
                            .update("viewers", FieldValue.arrayRemove(currentUser.uid))
                    }

                    Tasks.whenAll(tasks).addOnSuccessListener {
                        db.collection("tournaments")
                            .whereEqualTo("ownerId", currentUser.uid)
                            .get()
                            .addOnSuccessListener { ownerQuerySnapshot ->
                                ownerQuerySnapshot.documents.forEach { document ->
                                    FirebaseHelper.deleteSubcollectionsAndDocument(document.id)
                                }

                                FirebaseHelper.deleteUserData(currentUser.uid) {
                                    currentUser.delete().addOnCompleteListener { task ->
                                        if (task.isSuccessful) onSuccess()
                                        else onError(task.exception?.message.orEmpty())
                                    }
                                }
                            }
                    }
                }
        } ?: onError("No user is currently signed in.")
    }
}
