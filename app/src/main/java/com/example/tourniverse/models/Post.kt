package com.example.tourniverse.models

// Data model for a social post
data class Post(
    val userName: String,
    val content: String,
    var likes: Int = 0, // Number of likes
    val comments: MutableList<String> = mutableListOf() // List of comments
)
