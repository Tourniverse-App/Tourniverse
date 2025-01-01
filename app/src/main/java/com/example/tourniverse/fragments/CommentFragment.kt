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
import com.example.tourniverse.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CommentFragment : Fragment() {

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentInput: EditText
    private lateinit var sendCommentButton: ImageView
    private val comments = mutableListOf<Comment>()
    private lateinit var adapter: CommentAdapter

    private val db = FirebaseFirestore.getInstance()
    private lateinit var postId: String
    private lateinit var tournamentId: String
    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_comment, container, false)

        // Retrieve arguments
        postId = arguments?.getString("postId") ?: ""
        tournamentId = arguments?.getString("tournamentId") ?: ""

        if (postId.isEmpty() || tournamentId.isEmpty()) {
            Toast.makeText(context, "Error: Invalid Post ID or Tournament ID!", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }

        // Initialize views
        commentsRecyclerView = view.findViewById(R.id.commentsRecyclerView)
        commentInput = view.findViewById(R.id.commentInput)
        sendCommentButton = view.findViewById(R.id.sendCommentButton)

        // RecyclerView setup
        adapter = CommentAdapter(comments)
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        commentsRecyclerView.adapter = adapter

        // Load comments
        fetchComments(postId)

        // Send button logic
        sendCommentButton.setOnClickListener {
            val commentText = commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
                commentInput.text.clear()
            } else {
                Toast.makeText(context, "Comment cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove() // Remove listener when fragment is destroyed
    }

    /**
     * Fetches comments for the post in real-time.
     */
    private fun fetchComments(postId: String) {
        val commentsRef = db.collection("tournaments")
            .document(tournamentId)
            .collection("chat")
            .document(postId)

        listener = commentsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("CommentFragment", "Error fetching comments: ${e.message}")
                return@addSnapshotListener
            }

            snapshot?.let {
                val fetchedComments = it.get("comments") as? List<Map<String, Any>> ?: emptyList()
                comments.clear()
                fetchedComments.forEach { commentMap ->
                    val userId = commentMap["userId"] as? String ?: ""
                    val username = commentMap["username"] as? String ?: ""
                    val text = commentMap["commentText"] as? String ?: ""
                    val createdAt = (commentMap["createdAt"] as? Long) ?: 0L

                    comments.add(Comment(userId, username, text, createdAt))
                }
                adapter.notifyDataSetChanged()
                commentsRecyclerView.scrollToPosition(comments.size - 1)
            }
        }
    }

    /**
     * Adds a new comment to Firebase.
     */
    private fun addComment(commentText: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: "Unknown"

        val newComment = Comment(userId, commentText)

        FirebaseHelper.addCommentToPost(postId, newComment, tournamentId)
        Toast.makeText(context, "Comment added!", Toast.LENGTH_SHORT).show()
    }
}
