package com.example.tourniverse.models

data class Tournament(
    val id: String,
    val name: String,
    val type: String,
    val format: String? = null,
    val description: String? = null,
    val teamNames: List<String>? = null,
    val owner: String? = null,
    val viewers: List<String>? = null,
    val memberCount: Int = 1 // Default to 1
)
