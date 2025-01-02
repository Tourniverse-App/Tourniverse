package com.example.tourniverse.adapters

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.fragments.CommentFragment
import com.example.tourniverse.models.ChatMessage
import com.example.tourniverse.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatAdapter(
    private val context: Context,
    private val messages: MutableList<ChatMessage>,
    private val tournamentId: String
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    // ViewHolder class
    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        val likeButton: ImageView = itemView.findViewById(R.id.likeButton)
        val likeCountTextView: TextView = itemView.findViewById(R.id.likeCountTextView)
        val commentButton: ImageView = itemView.findViewById(R.id.commentButton)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
    }

    // Create ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return ChatViewHolder(view)
    }

    // Bind data to views
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        // Set user name
        holder.userNameTextView.text = message.senderName

        // Set content
        holder.contentTextView.text = message.message

        // Format and set timestamp
        val time = android.text.format.DateFormat.format("hh:mm a", message.createdAt)
        holder.timestampTextView.text = time.toString()

        // Get current user ID
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Dynamically fetch document ID
        val db = FirebaseFirestore.getInstance()
        val chatRef = db.collection("tournaments").document(tournamentId).collection("chat")

        chatRef.whereEqualTo("message", message.message)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val documentId = documents.documents[0].id // Retrieve the document ID

                    // Fetch the latest data from Firestore
                    chatRef.document(documentId).get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            // Get likes and likedBy fields
                            val likesCount = snapshot.getLong("likesCount")?.toInt() ?: 0
                            val likedBy = snapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()
                            val isLiked = likedBy.contains(currentUserId)

                            // Update UI with latest data
                            holder.likeCountTextView.text = "$likesCount Likes"
                            holder.likeButton.setImageResource(
                                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                            )

                            // Like button click listener
                            holder.likeButton.setOnClickListener {
                                db.runTransaction { transaction ->
                                    val updatedSnapshot = transaction.get(chatRef.document(documentId))
                                    val updatedLikedBy = updatedSnapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()
                                    val updatedLikesCount = updatedSnapshot.getLong("likesCount")?.toInt() ?: 0

                                    if (updatedLikedBy.contains(currentUserId)) {
                                        // Unlike the post
                                        updatedLikedBy.remove(currentUserId)
                                        transaction.update(chatRef.document(documentId), "likedBy", updatedLikedBy)
                                        transaction.update(chatRef.document(documentId), "likesCount", updatedLikesCount - 1)

                                        // Update UI directly
                                        holder.likeButton.setImageResource(R.drawable.ic_heart_outline)
                                        holder.likeCountTextView.text = "${updatedLikesCount - 1} Likes"
                                    } else {
                                        // Like the post
                                        updatedLikedBy.add(currentUserId)
                                        transaction.update(chatRef.document(documentId), "likedBy", updatedLikedBy)
                                        transaction.update(chatRef.document(documentId), "likesCount", updatedLikesCount + 1)

                                        // Update UI directly
                                        holder.likeButton.setImageResource(R.drawable.ic_heart_filled)
                                        holder.likeCountTextView.text = "${updatedLikesCount + 1} Likes"
                                    }
                                }.addOnFailureListener { e ->
                                    Log.e("ChatAdapter", "Failed to toggle like: ${e.message}")
                                    Toast.makeText(context, "Failed to update like!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.e("ChatAdapter", "Failed to fetch updated document: ${e.message}")
                    }
                } else {
                    Log.e("ChatAdapter", "Document not found for message: ${message.message}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatAdapter", "Failed to fetch document ID: ${e.message}")
            }

        // Comment button click listener
        holder.commentButton.setOnClickListener {
            val commentFragment = CommentFragment()
            val bundle = Bundle()

            // Pass the document ID directly as an argument
            bundle.putString("documentId", message.documentId) // Correct ID
            bundle.putString("tournamentId", tournamentId)
            commentFragment.arguments = bundle

            val navController = (context as FragmentActivity)
                .supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment)?.findNavController()

            navController?.navigate(
                R.id.commentFragment,
                bundle
            )
        }
    }

    // Item count
    override fun getItemCount(): Int = messages.size
}