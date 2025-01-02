package com.example.tourniverse.models

data class Match(
    val teamA: String = "",     // Team A name
    val teamB: String = "",     // Team B name
    val scoreA: Int = 0,        // Score for Team A
    val scoreB: Int = 0,        // Score for Team B
    val playedAt: String = "", // owner chooses a date, DD:MM HH:MM)
    val id: String = ""         // Firestore ID
)

