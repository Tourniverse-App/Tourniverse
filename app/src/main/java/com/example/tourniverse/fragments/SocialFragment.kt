package com.example.tourniverse.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tourniverse.R
import com.example.tourniverse.adapters.ChatAdapter
import com.example.tourniverse.models.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SocialFragment : Fragment() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendIcon: ImageView
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val db = FirebaseFirestore.getInstance()
    private var tournamentId: String? = null
    private lateinit var chatCollection: Query

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_social, container, false)

        // Retrieve tournamentId from arguments
        tournamentId = arguments?.getString("tournamentId")
        if (!tournamentId.isNullOrEmpty()) {
            Log.d("SocialFragment", "Tournament ID: $tournamentId")
            chatCollection = db.collection("tournaments")
                .document(tournamentId!!)
                .collection("chat")
                .orderBy("createdAt", Query.Direction.ASCENDING)
        } else {
            Log.e("SocialFragment", "Tournament ID is null or empty. Defaulting to fallback chat.")
            chatCollection = db.collection("tournaments")
                .document("default")
                .collection("chat")
                .orderBy("createdAt", Query.Direction.ASCENDING)
        }

        // Initialize views
        chatRecyclerView = view.findViewById(R.id.postsRecyclerView)
        messageInput = view.findViewById(R.id.newPostInput)
        sendIcon = view.findViewById(R.id.sendPostButton)

        // RecyclerView setup
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ChatAdapter(chatMessages)
        chatRecyclerView.adapter = adapter

        // Send button logic
        sendIcon.setOnClickListener {
            val messageContent = messageInput.text.toString().trim()
            if (messageContent.isNotEmpty()) {
                sendMessage(messageContent)
                messageInput.text.clear()
            }
        }

        // Fetch messages if collection is valid
        fetchMessages()

        return view
    }

    private fun sendMessage(content: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: "Unknown"

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userSnapshot ->
                val username = userSnapshot.getString("username") ?: "Anonymous"
                val message = ChatMessage(
                    senderId = userId,
                    senderName = username,
                    message = content,
                    createdAt = System.currentTimeMillis()
                )

                db.collection("tournaments")
                    .document(tournamentId ?: "default")
                    .collection("chat")
                    .add(message)
                    .addOnSuccessListener {
                        Log.d("SocialFragment", "Message sent successfully: $message")
                    }
                    .addOnFailureListener { e ->
                        Log.e("SocialFragment", "Error sending message: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("SocialFragment", "Error fetching username for userId: $userId, ${e.message}")
            }
    }

    private fun fetchMessages() {
        chatCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("SocialFragment", "Error fetching messages: ${e.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                chatMessages.clear()
                snapshot.documents.forEach { document ->
                    val message = document.toObject(ChatMessage::class.java)
                    if (message != null) {
                        chatMessages.add(message)
                    }
                }
                adapter.notifyDataSetChanged()
                chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                Log.d("SocialFragment", "Messages updated: ${chatMessages.size}")
            }
        }
    }
}
