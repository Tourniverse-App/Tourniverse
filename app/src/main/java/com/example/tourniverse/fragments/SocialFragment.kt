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
    ): View {
        val view = inflater.inflate(R.layout.fragment_social, container, false)
        tournamentId = arguments?.getString("tournamentId")
        initializeViews(view)
        setupChatCollection()
        fetchMessages()
        return view
    }

    private fun initializeViews(view: View) {
        chatRecyclerView = view.findViewById(R.id.postsRecyclerView)
        messageInput = view.findViewById(R.id.newPostInput)
        sendIcon = view.findViewById(R.id.sendPostButton)

        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ChatAdapter(
            requireContext(),
            chatMessages,
            tournamentId.orEmpty()
        )
        chatRecyclerView.adapter = adapter

        sendIcon.setOnClickListener {
            val messageContent = messageInput.text.toString().trim()
            if (messageContent.isNotEmpty()) {
                sendMessage(messageContent)
                messageInput.text.clear()
            }
        }
    }

    private fun setupChatCollection() {
        tournamentId = tournamentId ?: fetchFallbackTournamentId()
        chatCollection = db.collection("tournaments")
            .document(tournamentId.orEmpty())
            .collection("chat")
            .orderBy("createdAt", Query.Direction.ASCENDING)
    }

    private fun fetchFallbackTournamentId(): String {
        var fallbackId = "default"
        db.collection("tournaments")
            .get()
            .addOnSuccessListener { documents ->
                fallbackId = documents.firstOrNull()?.id ?: fallbackId
                Log.d("SocialFragment", "Fallback Tournament ID fetched: $fallbackId")
            }
            .addOnFailureListener { e ->
                Log.e("SocialFragment", "Error fetching fallback Tournament ID: ${e.message}")
            }
        return fallbackId
    }

    private fun sendMessage(content: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid.orEmpty()

        if (tournamentId.isNullOrEmpty()) {
            showToast("Cannot send message without a valid tournament ID.")
            return
        }

        val filteredContent = filterProfanity(content)
        fetchUserProfile(userId) { username, profilePhoto ->
            val message = ChatMessage(
                senderId = userId,
                senderName = username,
                profilePhoto = profilePhoto,
                message = filteredContent,
                createdAt = System.currentTimeMillis(),
                likesCount = 0,
                likedBy = mutableListOf(),
                comments = mutableListOf()
            )
            db.collection("tournaments").document(tournamentId!!)
                .collection("chat").add(message)
                .addOnSuccessListener {
                    notifyAllTournamentUsers(userId, username, filteredContent)
                }
                .addOnFailureListener { e ->
                    Log.e("SocialFragment", "Error sending message: ${e.message}")
                }
        }
    }

    private fun fetchUserProfile(userId: String, callback: (String, String?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                val username = snapshot.getString("username") ?: "Anonymous"
                val profilePhoto = snapshot.getString("profilePhoto") // Expecting Base64 string
                Log.d("SocialFragment", "Fetched username: $username, profilePhoto length: ${profilePhoto?.length}")
                callback(username, profilePhoto)
            }
            .addOnFailureListener { e ->
                Log.e("SocialFragment", "Error fetching user profile: ${e.message}")
                callback("Anonymous", null)
            }
    }

    private fun filterProfanity(content: String): String {
        val bannedWords = loadBannedWords()
        return content.split(" ").joinToString(" ") { word ->
            if (bannedWords.contains(word.lowercase())) "***" else word
        }
    }

    private fun loadBannedWords(): List<String> {
        return try {
            requireContext().assets.open("Blacklist.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toList()
            }
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

            chatMessages.clear()
            snapshot?.documents?.mapNotNullTo(chatMessages) { it.toObject(ChatMessage::class.java)?.apply { documentId = it.id } }
            adapter.notifyDataSetChanged()
            chatRecyclerView.scrollToPosition(chatMessages.size - 1)
            Log.d("SocialFragment", "Messages updated: ${chatMessages.size}")
        }
    }

    private fun notifyAllTournamentUsers(senderId: String, senderName: String, message: String) {
        db.collection("tournaments").document(tournamentId.orEmpty()).get()
            .addOnSuccessListener { snapshot ->
                val tournamentName = snapshot.getString("name") ?: "Tournament"
                val title = tournamentName
                val body = "$senderName: $message"

                db.collection("tournaments").document(tournamentId.orEmpty())
                    .collection("viewers").get()
                    .addOnSuccessListener { viewersSnapshot ->
                        viewersSnapshot.documents
                            .mapNotNull { it.id }
                            .filter { it != senderId }
                            .forEach { userId ->
                                sendNotification(userId, "ChatMessages", title, body)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("SocialFragment", "Failed to fetch viewers: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("SocialFragment", "Failed to fetch tournament details: ${e.message}")
            }
    }

    private fun sendNotification(userId: String, type: String, title: String, body: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                val fcmToken = snapshot.getString("fcmToken") ?: return@addOnSuccessListener
                Log.d("FCM Notification", "Sending notification to $fcmToken: $title - $body")
                // Implement FCM notification logic here (e.g., Retrofit or HTTP client)
            }
            .addOnFailureListener { e ->
                Log.e("SocialFragment", "Failed to fetch FCM token: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
