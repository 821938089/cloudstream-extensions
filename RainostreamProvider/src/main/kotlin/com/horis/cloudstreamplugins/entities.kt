package com.horis.cloudstreamplugins


data class PEEvents(
    val events: List<PEEvent>
)


data class PEEvent(
    val __v: Int,
    val _id: String,
    val compitition: String,
    val kickOff: String,
    val status: String,
    val stream: String,
    val title: String
)

data class RugbyGames(
    val game: List<RugbyGame>
)

data class RugbyGame(
    val away_team_name: String,
    val date: String,
    val home_team_name: String,
    val id: Int,
    val stream: String,
    val time: String,
    val tournaments: String
)
