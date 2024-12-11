package com.example.tourniverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.Post

class PostAdapter(private val posts: MutableList<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userNameTextView: TextView = view.findViewById(R.id.userNameTextView)
        val contentTextView: TextView = view.findViewById(R.id.contentTextView)
        val likeButton: TextView = view.findViewById(R.id.likeButton)
        val likeCountTextView: TextView = view.findViewById(R.id.likeCountTextView)
        val commentButton: TextView = view.findViewById(R.id.commentButton)
        val commentsSection: ViewGroup = view.findViewById(R.id.commentsSection)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        holder.userNameTextView.text = post.userName
        holder.contentTextView.text = post.content
        holder.likeCountTextView.text = "${post.likes} Likes"

        holder.likeButton.setOnClickListener {
            post.likes++
            holder.likeCountTextView.text = "${post.likes} Likes"
        }

        holder.commentButton.setOnClickListener {
            val newComment = "Example comment ${post.comments.size + 1}"
            post.comments.add(newComment)
            val commentTextView = TextView(holder.itemView.context).apply {
                text = newComment
                textSize = 14f
                setPadding(8, 8, 8, 8)
            }
            holder.commentsSection.addView(commentTextView)
        }

        // Display existing comments
        holder.commentsSection.removeAllViews()
        post.comments.forEach { comment ->
            val commentTextView = TextView(holder.itemView.context).apply {
                text = comment
                textSize = 14f
                setPadding(8, 8, 8, 8)
            }
            holder.commentsSection.addView(commentTextView)
        }
    }

    override fun getItemCount(): Int = posts.size
}
