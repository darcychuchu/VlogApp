package com.vlog.app.data.videos

data class Gather(
    val id: String,
    val title: String,
    val playerDomain: String,
    val countVideo: String,
    val typed: String,
    val videoPlayersList: List<Any> = emptyList()
)

data class GatherResponse(
    val data: List<Gather>
)
