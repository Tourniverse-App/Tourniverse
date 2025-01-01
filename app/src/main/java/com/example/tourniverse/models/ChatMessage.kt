package com.example.tourniverse.models

import com.google.firebase.firestore.PropertyName

data class ChatMessage(
    @get:PropertyName("senderId") @set:PropertyName("senderId") var senderId: String = "",
    @get:PropertyName("senderName") @set:PropertyName("senderName") var senderName: String = "",
    @get:PropertyName("message") @set:PropertyName("message") var message: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long = 0L,
    @get:PropertyName("likesCount") @set:PropertyName("likesCount") var likesCount: Int = 0,
    @get:PropertyName("likedBy") @set:PropertyName("likedBy") var likedBy: MutableList<String> = mutableListOf(),
    @get:PropertyName("comments") @set:PropertyName("comments") var comments: MutableList<Comment> = mutableListOf()
)