package com.example.tourniverse.models

data class Comment(
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)