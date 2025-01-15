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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.CommentAdapter
import com.example.tourniverse.models.Comment
import com.example.tourniverse.viewmodels.CommentViewModel
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
    private lateinit var viewModel: CommentViewModel
    private val comments = mutableListOf<Comment>()

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

        viewModel = ViewModelProvider(this).get(CommentViewModel::class.java)

        // Initialize views
        commentsRecyclerView = view.findViewById(R.id.commentsRecyclerView)
        commentInput = view.findViewById(R.id.commentInput)
        sendCommentButton = view.findViewById(R.id.sendCommentButton)
        backButton = view.findViewById(R.id.backButton)

        setupRecyclerView()
        observeComments()

        sendCommentButton.setOnClickListener { handleSendComment() }
        backButton.setOnClickListener { requireActivity().onBackPressed() }

        viewModel.fetchComments(tournamentId, documentId)

        return view
    }

    private fun setupRecyclerView() {
        adapter = CommentAdapter(comments)
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        commentsRecyclerView.adapter = adapter
    }

    private fun observeComments() {
        viewModel.comments.observe(viewLifecycleOwner) { updatedComments ->
            comments.clear()
            comments.addAll(updatedComments)
            adapter.notifyDataSetChanged()
            commentsRecyclerView.scrollToPosition(comments.size - 1)
        }
    }

    private fun handleSendComment() {
        val commentText = commentInput.text.toString().trim()
        if (commentText.isEmpty()) {
            showToast("Comment cannot be empty!")
            return
        }
        commentInput.text.clear()

        viewModel.addComment(
            tournamentId,
            documentId,
            commentText,
            requireContext()
        ) { success, error ->
            if (!success) {
                showToast("Failed to add comment: $error")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

