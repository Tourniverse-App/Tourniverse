package com.example.tourniverse.models

import com.google.firebase.firestore.PropertyName

data class ChatMessage(
    var documentId: String = "",
    var senderId: String = "",
    var senderName: String = "",
    var message: String = "",
    var profilePhoto: String? = null,
    var createdAt: Long = 0L,
    var likesCount: Int = 0,
    var likedBy: MutableList<String> = mutableListOf(),
    var comments: MutableList<Comment> = mutableListOf()
)

