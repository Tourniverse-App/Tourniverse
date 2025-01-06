package com.example.tourniverse.models

data class Match(
    val teamA: String = "",
    val teamB: String = "",
    var scoreA: Int? = null,     // Changed to nullable Int
    var scoreB: Int? = null,     // Changed to nullable Int
    val playedAt: String = "",
    val id: String = ""
)

