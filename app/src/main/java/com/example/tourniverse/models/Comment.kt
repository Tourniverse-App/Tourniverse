package com.example.tourniverse.models

data class Comment(
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
    val profilePhoto: String? = null // Optional field for profile photo
)
