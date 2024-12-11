package com.example.tourniverse.models

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profileImage: String = "",
    val tournaments: List<String> = emptyList() // List of tournament IDs
)
