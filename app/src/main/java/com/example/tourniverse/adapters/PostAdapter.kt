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

class PostAdapter(
    private val context: Context,
    private val posts: MutableList<ChatMessage>,
    private val tournamentId: String // Added tournamentId for navigation
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userNameTextView: TextView = view.findViewById(R.id.userNameTextView)
        val contentTextView: TextView = view.findViewById(R.id.contentTextView)
        val likeButton: ImageView = view.findViewById(R.id.likeButton)
        val likeCountTextView: TextView = view.findViewById(R.id.likeCountTextView)
        val commentButton: ImageView = view.findViewById(R.id.commentButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Display post details
        holder.userNameTextView.text = post.senderName
        holder.contentTextView.text = post.message
        holder.likeCountTextView.text = "${post.likesCount} Likes"

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Update heart button state
        val isLiked = post.likedBy.contains(currentUserId)
        holder.likeButton.setImageResource(
            if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )

        // Handle like button click
        holder.likeButton.setOnClickListener {
            if (currentUserId == null) return@setOnClickListener

            if (post.likedBy.contains(currentUserId)) {
                post.likedBy.remove(currentUserId)
                post.likesCount--
            } else {
                post.likedBy.add(currentUserId)
                post.likesCount++
            }

            // Update UI
            holder.likeCountTextView.text = "${post.likesCount} Likes"
            holder.likeButton.setImageResource(
                if (post.likedBy.contains(currentUserId)) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )

            // Update Firebase
            FirebaseHelper.updatePostLikes(
                postId = post.senderId,
                likesCount = post.likesCount,
                likedBy = post.likedBy,
                tournamentId = tournamentId // Pass tournamentId for database path
            )
        }

        // Handle comment button click
        holder.commentButton.setOnClickListener {
            // Create CommentFragment and pass arguments
            val commentFragment = CommentFragment()
            val bundle = Bundle()
            bundle.putString("postId", post.senderId)
            bundle.putString("tournamentId", tournamentId)
            commentFragment.arguments = bundle

            // Navigate to CommentFragment
            val navController = (context as FragmentActivity)
                .supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment)?.findNavController()

            navController?.navigate(
                R.id.commentFragment, // Replace with the ID of your CommentFragment in the nav_graph.xml
                Bundle().apply {
                    putString("postId", post.senderId)
                    putString("tournamentId", tournamentId)
                }
            )

        }
    }

    override fun getItemCount(): Int = posts.size
}
