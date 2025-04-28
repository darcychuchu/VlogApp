package com.vlog.app.data.categories

import com.squareup.moshi.Json

/**
 * 分类数据模型
 */
data class Category(
    val id: String,
    val title: String,
    val path: String?,
    val description: String?,
    val isTyped: String?,
    val createdBy: String?,
    val modelId: String?,
    val parentId: String?,
    val orderSort: Int = 0,
    @Json(name = "categoryList")
    val categoryList: List<Category> = emptyList()
)