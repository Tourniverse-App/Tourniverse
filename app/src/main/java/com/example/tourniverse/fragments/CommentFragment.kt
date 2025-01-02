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

class CommentFragment : Fragment() {

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentInput: EditText
    private lateinit var sendCommentButton: ImageView
    private lateinit var backButton: ImageView
    private val comments = mutableListOf<Comment>()
    private lateinit var adapter: CommentAdapter

    private val db = FirebaseFirestore.getInstance()
    private var documentId: String = ""
    private var tournamentId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comment, container, false)

        // Retrieve arguments
        documentId = arguments?.getString("documentId") ?: ""
        tournamentId = arguments?.getString("tournamentId") ?: ""

        if (documentId.isEmpty() || tournamentId.isEmpty()) {
            Toast.makeText(context, "Invalid Message or Tournament ID", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }

        // Initialize views
        commentsRecyclerView = view.findViewById(R.id.commentsRecyclerView)
        commentInput = view.findViewById(R.id.commentInput)
        sendCommentButton = view.findViewById(R.id.sendCommentButton)
        backButton = view.findViewById(R.id.backButton)

        // Set up RecyclerView
        adapter = CommentAdapter(comments)
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        commentsRecyclerView.adapter = adapter

        // Load comments
        fetchComments()

        // Send button click listener
        sendCommentButton.setOnClickListener {
            val commentText = commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
                commentInput.text.clear()
            } else {
                Toast.makeText(context, "Comment cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        // Back button click listener
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        return view
    }

    /**
     * Fetch comments in real-time.
     */
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
                    val fetchedComments = document.get("comments") as? List<HashMap<String, Any>> ?: emptyList()
                    comments.clear()

                    fetchedComments.forEach { commentMap ->
                        val userId = commentMap["userId"] as? String ?: ""
                        val text = commentMap["text"] as? String ?: ""
                        val createdAt = (commentMap["createdAt"] as? Long) ?: 0L

                        // Fetch username from userId
                        db.collection("users").document(userId).get()
                            .addOnSuccessListener { userSnapshot ->
                                val username = userSnapshot.getString("username") ?: "Unknown"

                                // Add comment to the list with username
                                comments.add(Comment(userId, username, text, createdAt))
                                adapter.notifyDataSetChanged()
                                commentsRecyclerView.scrollToPosition(comments.size - 1)
                            }
                            .addOnFailureListener { error ->
                                Log.e("CommentFragment", "Failed to fetch username: ${error.message}")
                            }
                    }
                }
            }
    }


    /**
     * Adds a comment to Firestore.
     */
    private fun addComment(commentText: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Fetch the username from the user's profile in Firestore
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userSnapshot ->
                val username = userSnapshot.getString("username") ?: "Anonymous" // Default if username is missing

                // Create a new comment object
                val newComment = hashMapOf(
                    "userId" to userId,
                    "username" to username,
                    "text" to commentText,
                    "createdAt" to System.currentTimeMillis()
                )

                // Reference the document
                val postRef = db.collection("tournaments")
                    .document(tournamentId)
                    .collection("chat")
                    .document(documentId)

                postRef.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // If the comments field does not exist, create it
                            if (!document.contains("comments")) {
                                postRef.set(mapOf("comments" to listOf(newComment)), SetOptions.merge())
                            } else {
                                // Append to existing comments array
                                postRef.update("comments", FieldValue.arrayUnion(newComment))
                            }
                            Log.d("CommentFragment", "Comment added successfully!")
                            Toast.makeText(context, "Comment added!", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("CommentFragment", "Message not found!")
                            Toast.makeText(context, "Message not found!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("CommentFragment", "Error fetching message: ${e.message}")
                        Toast.makeText(context, "Failed to fetch message!", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("CommentFragment", "Failed to fetch username: ${e.message}")
                Toast.makeText(context, "Failed to fetch username!", Toast.LENGTH_SHORT).show()
            }
    }

}
