package com.example.tourniverse.models

import com.google.firebase.firestore.PropertyName

data class ChatMessage(
    var documentId: String = "", // Add this field to store the Firestore ID
    var senderId: String = "",
    var senderName: String = "",
    var message: String = "",
    var createdAt: Long = 0L,
    var likesCount: Int = 0,
    var likedBy: MutableList<String> = mutableListOf(),
    var comments: MutableList<Comment> = mutableListOf()
)

