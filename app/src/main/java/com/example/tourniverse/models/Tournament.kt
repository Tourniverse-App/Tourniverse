package com.example.tourniverse.models

data class Tournament(
    val name: String,
    val type: String,
    val format: String? = null,          // Format of the tournament
    val description: String? = null,    // Tournament description
    val teamNames: List<String>? = null, // List of team names
    val owner: String? = null,           // Owner of the tournament
    val viewers: List<String>? = null    // List of viewers
)
