package com.vlog.app.data.histories.watch

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val cover: String,
    val duration: String?,
    val watchedDuration: Long,
    val lastWatchedTime: Date,
    val typed: Int
)