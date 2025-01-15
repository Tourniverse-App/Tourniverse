package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.CommentAdapter
import com.example.tourniverse.models.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentSnapshot

class CommentFragment : Fragment() {

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentInput: EditText
    private lateinit var sendCommentButton: ImageView
    private lateinit var backButton: ImageView
    private lateinit var adapter: CommentAdapter
    private val comments = mutableListOf<Comment>()

    private val db = FirebaseFirestore.getInstance()
    private var documentId: String = ""
    private var tournamentId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_comment, container, false)

        documentId = arguments?.getString("documentId").orEmpty()
        tournamentId = arguments?.getString("tournamentId").orEmpty()

        if (documentId.isEmpty() || tournamentId.isEmpty()) {
            showToast("Invalid Message or Tournament ID")
            requireActivity().onBackPressed()
        }

        // Initialize views
        commentsRecyclerView = view.findViewById(R.id.commentsRecyclerView)
        commentInput = view.findViewById(R.id.commentInput)
        sendCommentButton = view.findViewById(R.id.sendCommentButton)
        backButton = view.findViewById(R.id.backButton)

        setupRecyclerView()
        fetchComments()

        sendCommentButton.setOnClickListener { handleSendComment() }
        backButton.setOnClickListener { requireActivity().onBackPressed() }

        return view
    }

    private fun setupRecyclerView() {
        adapter = CommentAdapter(comments)
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        commentsRecyclerView.adapter = adapter
    }

    private fun fetchComments() {
        db.collection("tournaments")
            .document(tournamentId)
            .collection("chat")
            .document(documentId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CommentFragment", "Failed to fetch comments: ${e.message}")
                    return@addSnapshotListener
                }

                snapshot?.let { document ->
                    val fetchedComments = document.get("comments") as? List<Map<String, Any>> ?: emptyList()
                    comments.clear()

                    fetchedComments.forEach { commentMap ->
                        val userId = commentMap["userId"] as? String ?: ""
                        val text = commentMap["text"] as? String ?: ""
                        val createdAt = (commentMap["createdAt"] as? Long) ?: 0L

                        fetchUsername(userId) { username ->
                            comments.add(Comment(userId, username, text, createdAt))
                            adapter.notifyDataSetChanged()
                            commentsRecyclerView.scrollToPosition(comments.size - 1)
                        }
                    }
                }
            }
    }

    private fun handleSendComment() {
        val commentText = commentInput.text.toString().trim()
        if (commentText.isEmpty()) {
            showToast("Comment cannot be empty!")
            return
        }
        commentInput.text.clear()
        addComment(commentText)
    }

    private fun fetchUsername(userId: String, onResult: (String) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userSnapshot ->
                onResult(userSnapshot.getString("username") ?: "Unknown")
            }
            .addOnFailureListener { e ->
                Log.e("CommentFragment", "Failed to fetch username: ${e.message}")
            }
    }

    private fun addComment(commentText: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (userId.isEmpty()) return

        val filteredContent = applyProfanityFilter(commentText)
        fetchUsername(userId) { username ->
            val newComment = mapOf(
                "userId" to userId,
                "username" to username,
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
                    notifyOwnerIfNeeded(document, userId, username, filteredContent)
                } else {
                    showToast("Message not found!")
                }
            }.addOnFailureListener {
                Log.e("CommentFragment", "Error fetching message: ${it.message}")
            }
        }
    }

    private fun updateComments(postRef: com.google.firebase.firestore.DocumentReference, document: com.google.firebase.firestore.DocumentSnapshot, newComment: Map<String, Any>) {        if (!document.contains("comments")) {
            postRef.set(mapOf("comments" to listOf(newComment)), SetOptions.merge())
        } else {
            postRef.update("comments", FieldValue.arrayUnion(newComment))
        }
        showToast("Comment added!")
    }

    private fun notifyOwnerIfNeeded(document: com.google.firebase.firestore.DocumentSnapshot, userId: String, username: String, filteredContent: String) {        val senderId = document.getString("senderId") ?: return
        if (senderId != userId) {
            notifyMessageOwner(senderId, username, filteredContent)
        }
    }

    private fun applyProfanityFilter(content: String): String {
        val bannedWords = loadBannedWords()
        return content.split(" ").joinToString(" ") { word ->
            if (bannedWords.contains(word.lowercase())) "***" else word
        }
    }

    private fun loadBannedWords(): List<String> {
        return try {
            requireContext().assets.open("Blacklist.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toList()
            }
        } catch (e: Exception) {
            Log.e("Blacklist", "Error loading banned words: ${e.message}")
            emptyList()
        }
    }

    private fun notifyMessageOwner(senderId: String, commenterName: String, commentText: String) {
        db.collection("users").document(senderId)
            .collection("notifications").document("settings")
            .get()
            .addOnSuccessListener { settingsSnapshot ->
                val pushEnabled = settingsSnapshot.getBoolean("Push") ?: false
                val commentsEnabled = settingsSnapshot.getBoolean("Comments") ?: false

                if (pushEnabled && commentsEnabled) {
                    sendNotification(senderId, commenterName, commentText)
                }
            }
    }

    private fun sendNotification(senderId: String, commenterName: String, commentText: String) {
        db.collection("users").document(senderId)
            .get()
            .addOnSuccessListener { userSnapshot ->
                val fcmToken = userSnapshot.getString("fcmToken")
                if (!fcmToken.isNullOrEmpty()) {
                    sendFCMNotification(fcmToken, "New Comment on Your Message!", "$commenterName commented: \"$commentText\"")
                }
            }
    }

    private fun sendFCMNotification(fcmToken: String, title: String, body: String) {
        val notificationData = mapOf(
            "to" to fcmToken,
            "notification" to mapOf("title" to title, "body" to body),
            "data" to mapOf("type" to "Comment", "tournamentId" to tournamentId, "messageId" to documentId)
        )
        Log.d("FCM Notification", "Sending notification: $notificationData")
        // Add HTTP client logic to send notification
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
