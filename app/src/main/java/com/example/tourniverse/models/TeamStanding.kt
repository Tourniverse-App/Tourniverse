package com.example.tourniverse.models

data class TeamStanding(
    val teamName: String = "",
    var points: Int = 0,
    var goals: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0
)
