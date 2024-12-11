package com.example.tourniverse.models

data class Tournament(
    val name: String,
    val type: String,
    val format: String? = null,          // Add format
    val description: String? = null,    // Add description
    val teamNames: List<String>? = null // Add team names
)
