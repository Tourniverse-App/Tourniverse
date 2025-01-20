package com.example.tourniverse.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tourniverse.R
import com.example.tourniverse.models.Comment

class CommentAdapter(private val comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.commentUserNameTextView)
        val commentTextView: TextView = itemView.findViewById(R.id.commentTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.commentTimestampTextView)
        val userImageView: ImageView = itemView.findViewById(R.id.userImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        // Set username
        holder.userNameTextView.text = comment.username

        // Set comment text
        holder.commentTextView.text = comment.text

        // Format and set timestamp
        holder.timestampTextView.text = formatTimestamp(comment.createdAt)

        // Load profile image
        val profilePhoto = comment.profilePhoto
        if (!profilePhoto.isNullOrEmpty()) {
            try {
                // Decode Base64-encoded photo
                val decodedBytes = android.util.Base64.decode(profilePhoto, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                // Set decoded bitmap to ImageView
                holder.userImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("CommentAdapter", "Error decoding profile photo: ${e.message}")
                // Fallback to default placeholder if decoding fails
                holder.userImageView.setImageResource(R.drawable.ic_user)
            }
        } else {
            // Set default image if profilePhoto is null or empty
            holder.userImageView.setImageResource(R.drawable.ic_user)
        }

    }

    override fun getItemCount(): Int {
        return comments.size
    }

    /**
     * Formats the timestamp to a readable date/time format.
     */
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
