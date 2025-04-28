package com.vlog.app.data.categories

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类实体类，用于Room数据库存储
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val parentId: String?,
    val isTyped: String?,
    val description: String?,
    val path: String?,
    val createdBy: String?,
    val modelId: String?,
    val childrenIds: String = "",
    val orderSort: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)