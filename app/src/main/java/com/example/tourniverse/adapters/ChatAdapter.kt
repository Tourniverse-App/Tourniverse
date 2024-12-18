package com.example.tourniverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderNameTextView: TextView = itemView.findViewById(R.id.senderName)
        private val messageTextView: TextView = itemView.findViewById(R.id.messageContent)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestamp)

        fun bind(chatMessage: ChatMessage) {
            // Set the sender name with a fallback
            senderNameTextView.text = if (!chatMessage.senderName.isNullOrEmpty()) {
                chatMessage.senderName
            } else {
                "Anonymous"
            }

            // Set the message content
            messageTextView.text = chatMessage.message ?: "No content"

            // Format the timestamp and set it
            val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeString = try {
                dateFormat.format(Date(chatMessage.createdAt))
            } catch (e: Exception) {
                "Unknown Time"
            }
            timestampTextView.text = timeString
        }
    }
}
