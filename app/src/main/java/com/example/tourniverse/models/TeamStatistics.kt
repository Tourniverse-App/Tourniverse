package com.example.tourniverse.models

// Data model for team statistics
data class TeamStatistics(
    val teamName: String,
    val wins: Int,
    val losses: Int,
    val fieldGoals: Int,
    val points: Int
)
