package com.vlog.app.data

import com.vlog.app.data.videos.Video

data class FilterListResponse(
    val items: List<Video>,
    val total: String,
    val page: String,
    val pageSize: String
)