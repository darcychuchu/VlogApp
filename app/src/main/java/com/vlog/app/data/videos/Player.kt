package com.vlog.app.data.videos

data class Player(
    val id: String,
    val videoId: String,
    val gatherId: String,
    val videoTitle: String,
    val playerUrl: String
)

data class PlayerResponse(
    val data: List<Player>
)
