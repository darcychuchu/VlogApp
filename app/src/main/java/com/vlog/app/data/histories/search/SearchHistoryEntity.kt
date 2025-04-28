package com.vlog.app.data.histories.search

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.vlog.app.data.Converters
import java.util.Date

/**
 * 搜索历史实体类
 */
@Entity(tableName = "search_history")
@TypeConverters(Converters::class)
data class SearchHistoryEntity(
    @PrimaryKey
    val query: String,
    val timestamp: Date
)
