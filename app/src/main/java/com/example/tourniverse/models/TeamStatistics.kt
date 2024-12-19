package com.example.tourniverse.models

// Data model for team statistics
data class TeamStatistics(
    val teamName: String = "",
    var wins: Int = 0,
    var draws: Int = 0, // Added to track draws
    var losses: Int = 0,
    var goalsFor: Int = 0, // Goals scored by the team
    var goalsAgainst: Int = 0, // Goals conceded by the team
    var points: Int = 0
)
