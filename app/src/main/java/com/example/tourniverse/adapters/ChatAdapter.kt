package com.example.tourniverse.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.fragments.CommentFragment
import com.example.tourniverse.models.ChatMessage
import com.example.tourniverse.utils.FirebaseHelper
import com.google.firebase.auth.FirebaseAuth

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

        // Handle likes
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val isLiked = message.likedBy.contains(currentUserId)

        // Update like button UI
        holder.likeButton.setImageResource(
            if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        holder.likeCountTextView.text = "${message.likesCount} Likes"

        // Like button click listener
        holder.likeButton.setOnClickListener {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            // Toggle like
            if (message.likedBy.contains(currentUserId)) {
                message.likedBy.remove(currentUserId)
                message.likesCount--
            } else {
                message.likedBy.add(currentUserId)
                message.likesCount++
            }

            // Update UI
            holder.likeCountTextView.text = "${message.likesCount} Likes"
            holder.likeButton.setImageResource(
                if (message.likedBy.contains(currentUserId)) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )

            // Update Firebase
            FirebaseHelper.updatePostLikes(
                postId = message.senderId,
                likesCount = message.likesCount,
                likedBy = message.likedBy,
                tournamentId = tournamentId
            )
        }

        // Handle comment button click
        holder.commentButton.setOnClickListener {
            // Navigate to CommentFragment
            val commentFragment = CommentFragment()
            val bundle = Bundle()
            bundle.putString("postId", message.senderId)
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