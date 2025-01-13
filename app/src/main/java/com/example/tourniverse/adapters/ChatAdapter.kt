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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatAdapter(
    private val context: Context,
    private val messages: MutableList<ChatMessage>,
    private val tournamentId: String
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

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
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    // Bind data to views
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        // Set user name and content
        holder.userNameTextView.text = message.senderName
        holder.contentTextView.text = message.message

        // Format and set timestamp
        val time = android.text.format.DateFormat.format("hh:mm a", message.createdAt)
        holder.timestampTextView.text = time.toString()

        // Get current user ID
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Use documentId if available, otherwise query Firestore
        val documentId = message.documentId ?: run {
            Log.e("ChatAdapter", "Missing documentId for message: ${message.message}")
            return
        }

        val chatRef = db.collection("tournaments").document(tournamentId).collection("chat")

        // Fetch Firestore data (optional if likes data is already in message object)
        chatRef.document(documentId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
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
                        // Optimistically update UI
                        if (isLiked) {
                            holder.likeButton.setImageResource(R.drawable.ic_heart_outline)
                            holder.likeCountTextView.text = "${likesCount - 1} Likes"
                        } else {
                            holder.likeButton.setImageResource(R.drawable.ic_heart_filled)
                            holder.likeCountTextView.text = "${likesCount + 1} Likes"
                        }

                        // Update Firestore
                        db.runTransaction { transaction ->
                            val updatedSnapshot = transaction.get(chatRef.document(documentId))
                            val updatedLikedBy = updatedSnapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()
                            val updatedLikesCount = updatedSnapshot.getLong("likesCount")?.toInt() ?: 0

                            if (updatedLikedBy.contains(currentUserId)) {
                                updatedLikedBy.remove(currentUserId)
                                transaction.update(chatRef.document(documentId), "likedBy", updatedLikedBy)
                                transaction.update(chatRef.document(documentId), "likesCount", updatedLikesCount - 1)
                            } else {
                                updatedLikedBy.add(currentUserId)
                                transaction.update(chatRef.document(documentId), "likedBy", updatedLikedBy)
                                transaction.update(chatRef.document(documentId), "likesCount", updatedLikesCount + 1)

                                // Trigger notification for the message author
                                sendLikeNotification(
                                    senderId = message.senderId,
                                    senderName = message.senderName,
                                    likerId = currentUserId,
                                    likerName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown"
                                )
                            }
                        }.addOnFailureListener { e ->
                            Log.e("ChatAdapter", "Failed to toggle like: ${e.message}")
                            Toast.makeText(context, "Failed to update like!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatAdapter", "Failed to fetch document: ${e.message}")
            }

        // Comment button click listener
        holder.commentButton.setOnClickListener {
            val commentFragment = CommentFragment()
            val bundle = Bundle()

            // Pass the document ID directly as an argument
            bundle.putString("documentId", documentId)
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


    /**
     * Sends a like notification to the author of the message using FCM.
     */
    private fun sendLikeNotification(
        senderId: String,
        senderName: String,
        likerId: String,
        likerName: String
    ) {
        db.collection("users").document(senderId)
            .get()
            .addOnSuccessListener { userSnapshot ->
                val fcmToken = userSnapshot.getString("fcmToken")
                if (fcmToken.isNullOrEmpty()) {
                    Log.e("ChatAdapter", "No FCM token found for user: $senderId")
                    return@addOnSuccessListener
                }

                db.collection("users").document(senderId)
                    .collection("notifications").document("settings")
                    .get()
                    .addOnSuccessListener { globalSettings ->
                        val pushEnabled = globalSettings.getBoolean("push") ?: false
                        val likesEnabled = globalSettings.getBoolean("Likes") ?: false

                        if (pushEnabled && likesEnabled) {
                            db.collection("users").document(senderId)
                                .collection("tournaments").document(tournamentId)
                                .get()
                                .addOnSuccessListener { tournamentSettings ->
                                    val tournamentPushEnabled = tournamentSettings.getBoolean("push") ?: false
                                    val tournamentLikesEnabled = tournamentSettings.getBoolean("Likes") ?: false

                                    if (tournamentPushEnabled && tournamentLikesEnabled) {
                                        val notificationTitle = "Your message was liked!"
                                        val notificationBody = "$likerName liked your message."

                                        // Use FirebaseMessagingService or an HTTP request to send FCM
                                        sendFCMNotification(
                                            fcmToken,
                                            notificationTitle,
                                            notificationBody,
                                            mapOf(
                                                "type" to "Likes",
                                                "tournamentId" to tournamentId
                                            )
                                        )
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ChatAdapter", "Failed to fetch tournament settings: ${e.message}")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatAdapter", "Failed to fetch global settings: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ChatAdapter", "Failed to fetch user: ${e.message}")
            }
    }

    /**
     * Sends an FCM notification to the specified FCM token.
     */
    private fun sendFCMNotification(
        fcmToken: String,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        val notificationData = mapOf(
            "to" to fcmToken,
            "notification" to mapOf(
                "title" to title,
                "body" to body
            ),
            "data" to data
        )

        // Log the notification for debugging purposes
        Log.d("FCM Notification", "Sending notification: $notificationData")

        // Use your HTTP client to send the notification (e.g., Retrofit or Volley)
        // Alternatively, forward to a server-side FCM integration if available
    }


    // Item count
    override fun getItemCount(): Int = messages.size
}