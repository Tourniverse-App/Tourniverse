package com.example.tourniverse.models

data class Match(
    val teamA: String = "",
    val teamB: String = "",
    var scoreA: Int? = null,
    var scoreB: Int? = null,
    val id: String = "",
    var date: String = ""
)

