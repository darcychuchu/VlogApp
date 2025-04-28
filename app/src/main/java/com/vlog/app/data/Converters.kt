package com.vlog.app.data

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room 数据库类型转换器
 */
class Converters {
    /**
     * 将 Date 转换为 Long 类型存储
     */
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    /**
     * 将 Long 类型转换为 Date
     */
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
}