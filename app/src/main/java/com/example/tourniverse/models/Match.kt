package com.example.tourniverse.models

data class Match(
    val id: String = "",        // Unique ID for the match
    val teamA: String = "",     // Team A name
    val teamB: String = "",     // Team B name
    val scoreA: Int = 0,        // Score for Team A
    val scoreB: Int = 0,        // Score for Team B
    val playedAt: Long = System.currentTimeMillis() // Added to track match timestamp
)
