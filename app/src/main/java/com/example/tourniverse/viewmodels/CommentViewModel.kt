package com.example.tourniverse.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tourniverse.models.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class CommentViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> get() = _comments

    fun fetchComments(tournamentId: String, documentId: String) {
        db.collection("tournaments")
            .document(tournamentId)
            .collection("chat")
            .document(documentId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CommentViewModel", "Failed to fetch comments: ${e.message}")
                    return@addSnapshotListener
                }

                val fetchedComments = snapshot?.get("comments") as? List<Map<String, Any>> ?: emptyList()
                val commentsList = mutableListOf<Comment>()

                fetchedComments.forEach { commentMap ->
                    val userId = commentMap["userId"] as? String ?: ""
                    val text = commentMap["text"] as? String ?: ""
                    val createdAt = (commentMap["createdAt"] as? Long) ?: 0L
                    fetchUserProfile(userId) { username, profilePhoto ->
                        commentsList.add(Comment(userId, username, text, createdAt, profilePhoto))
                        _comments.value = commentsList
                    }
                }
            }
    }

    private fun fetchUserProfile(userId: String, callback: (String, String?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                val username = snapshot.getString("username") ?: "Anonymous"
                val profilePhoto = snapshot.getString("profilePhoto") // Can be null or empty
                callback(username, profilePhoto)
            }
            .addOnFailureListener { e ->
                Log.e("CommentViewModel", "Failed to fetch user profile: ${e.message}")
                callback("Anonymous", null)
            }
    }

    private fun fetchUsername(userId: String, onResult: (String) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.getString("username") ?: "Unknown")
            }
            .addOnFailureListener { e ->
                Log.e("CommentViewModel", "Failed to fetch username: ${e.message}")
            }
    }

    fun addComment(
        tournamentId: String,
        documentId: String,
        commentText: String,
        context: Context,
        onResult: (Boolean, String?) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (userId.isEmpty()) {
            onResult(false, "User not authenticated")
            return
        }

        val filteredContent = applyProfanityFilter(context, commentText)

        // Fetch both username and profile photo
        fetchUserProfile(userId) { username, profilePhoto ->
            val newComment = mapOf<String, Any>(
                "userId" to userId,
                "username" to username,
                "profilePhoto" to (profilePhoto ?: ""),
                "text" to filteredContent,
                "createdAt" to System.currentTimeMillis()
            )

            val postRef = db.collection("tournaments")
                .document(tournamentId)
                .collection("chat")
                .document(documentId)

            postRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    updateComments(postRef, document, newComment)
                    onResult(true, null)
                } else {
                    onResult(false, "Message not found")
                }
            }.addOnFailureListener {
                Log.e("CommentViewModel", "Error fetching message: ${it.message}")
                onResult(false, it.message)
            }
        }
    }

    private fun updateComments(
        postRef: com.google.firebase.firestore.DocumentReference,
        document: com.google.firebase.firestore.DocumentSnapshot,
        newComment: Map<String, Any>
    ) {
        if (!document.contains("comments")) {
            postRef.set(mapOf("comments" to listOf(newComment)), SetOptions.merge())
        } else {
            postRef.update("comments", FieldValue.arrayUnion(newComment))
        }
    }

    private fun applyProfanityFilter(context: Context, content: String): String {
        val bannedWords = loadBannedWords(context)
        return content.split(" ").joinToString(" ") { word ->
            if (bannedWords.contains(word.lowercase())) "***" else word
        }
    }

    private fun loadBannedWords(context: Context): List<String> {
        return try {
            context.assets.open("Blacklist.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toList()
            }
        } catch (e: Exception) {
            Log.e("Blacklist", "Error loading banned words: ${e.message}")
            emptyList()
        }
    }
}
