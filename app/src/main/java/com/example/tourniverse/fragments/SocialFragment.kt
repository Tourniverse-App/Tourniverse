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
import com.example.tourniverse.adapters.ChatAdapter
import com.example.tourniverse.models.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.tourniverse.models.Comment

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

        // Log at the start of onCreateView
        Log.d("SocialFragment", "onCreateView called")

        // Retrieve tournamentId from arguments
        tournamentId = arguments?.getString("tournamentId")
        Log.d("SocialFragment", "Tournament ID from arguments: $tournamentId")

        if (tournamentId.isNullOrEmpty()) {
            Log.e("SocialFragment", "Tournament ID is missing. Cannot proceed.")
            db.collection("tournaments")
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        tournamentId = document.id
                        Log.d("SocialFragment", "Fetched fallback Tournament ID: $tournamentId")
                        setupChatCollection()
                        fetchMessages()
                        break
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("SocialFragment", "Error fetching tournaments for fallback ID: ${e.message}")
                    setupDefaultChatCollection()
                    fetchMessages()
                }
        } else {
            setupChatCollection()
            fetchMessages()
        }

        // Initialize views
        chatRecyclerView = view.findViewById(R.id.postsRecyclerView)
        messageInput = view.findViewById(R.id.newPostInput)
        sendIcon = view.findViewById(R.id.sendPostButton)

        // RecyclerView setup
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ChatAdapter(
            requireContext(),         // Pass Context using 'requireContext()'
            chatMessages,             // Pass the list of messages
            tournamentId ?: ""        // Pass the tournamentId (fallback to empty string if null)
        )
        chatRecyclerView.adapter = adapter

        // Log RecyclerView initialization
        Log.d("SocialFragment", "RecyclerView and adapter initialized")

        // Send button logic
        sendIcon.setOnClickListener {
            val messageContent = messageInput.text.toString().trim()
            if (messageContent.isNotEmpty()) {
                sendMessage(messageContent)
                messageInput.text.clear()
            }
        }

        try {
            val inputStream = requireContext().assets.open("Blacklist.txt")
            val content = inputStream.bufferedReader().use { it.readText() }
            Log.d("BlacklistDebug", "File content:\n$content")
        } catch (e: Exception) {
            Log.e("BlacklistDebug", "Failed to load file: ${e.message}")
        }

        return view
    }

    private fun setupChatCollection() {
        if (!tournamentId.isNullOrEmpty()) {
            try {
                chatCollection = db.collection("tournaments")
                    .document(tournamentId!!)
                    .collection("chat")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                Log.d("SocialFragment", "Chat collection set up for Tournament ID: $tournamentId")
            } catch (e: Exception) {
                Log.e("SocialFragment", "Error setting up chat collection: ${e.message}")
                setupDefaultChatCollection()
            }
        } else {
            Log.e("SocialFragment", "Tournament ID is null or empty. Setting up default chat collection.")
            setupDefaultChatCollection()
        }
    }


    private fun setupDefaultChatCollection() {
        chatCollection = db.collection("tournaments")
            .document("default")
            .collection("chat")
            .orderBy("createdAt", Query.Direction.ASCENDING)
        Log.d("SocialFragment", "Using default chat collection")
    }

    private fun sendMessage(content: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: "Unknown"

        if (tournamentId.isNullOrEmpty()) {
            Log.e("SocialFragment", "Cannot send message. Tournament ID is null or empty.")
            Toast.makeText(context, "Cannot send message without a valid tournament ID.", Toast.LENGTH_SHORT).show()
            return
        }

        // Load banned words from Blacklist.txt
        val bannedWords = loadBannedWords()
        Log.d("SocialFragment", "Loaded banned words: $bannedWords")

        // Profanity filter
        val filteredContent = content.split(" ").joinToString(" ") { word ->
            if (bannedWords.contains(word.lowercase())) "***" else word // Replace banned words with ***
        }

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userSnapshot ->
                val username = userSnapshot.getString("username") ?: "Anonymous"
                val message = ChatMessage(
                    senderId = userId,
                    senderName = username,
                    message = filteredContent,
                    createdAt = System.currentTimeMillis(),
                    likesCount = 0, // Added for likes
                    likedBy = mutableListOf(), // Added for likes tracking
                    comments = mutableListOf() // Added for comments tracking
                )

                db.collection("tournaments")
                    .document(tournamentId!!)
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

    private fun loadBannedWords(): List<String> {
        return try {
            // Open the Blacklist.txt file
            val inputStream = requireContext().assets.open("Blacklist.txt")
            val words = inputStream.bufferedReader().useLines { lines ->
                lines.map { it.trim().lowercase() } // Clean each word (trim spaces and lowercase)
                    .filter { it.isNotEmpty() }     // Ignore empty lines
                    .toList()
            }
            Log.d("Blacklist", "Loaded banned words: $words") // Log the loaded words
            words
        } catch (e: Exception) {
            Log.e("Blacklist", "Error loading banned words: ${e.message}")
            emptyList()
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
                    message?.documentId = document.id // Assign Firestore Document ID
                    if (message != null) chatMessages.add(message)
                }
                adapter.notifyDataSetChanged()
                chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                Log.d("SocialFragment", "Messages updated: ${chatMessages.size}")
            }
        }
    }
}