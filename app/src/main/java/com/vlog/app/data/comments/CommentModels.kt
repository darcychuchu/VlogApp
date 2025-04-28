package com.vlog.app.data.comments

/**
 * 评论数据模型
 * 包含所有评论相关的数据类
 */

/**
 * 评论
 */
data class Comment(
    val id: String,
    val createdAt: String,
    val isLocked: String? = null,
    val isTyped: String? = null,
    val createdBy: String? = null,
    val attachmentId: String? = null,
    val quoteId: String? = null,
    val parentId: String? = null,
    val title: String? = null,
    val description: String,
    val createdByItem: Any? = null,
    val content: String? = null,
    val userId: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val likes: Int = 0
)

/**
 * 评论请求
 */
data class CommentRequest(
    val content: String
)

/**
 * API响应包装类
 */
data class CommentResponse(
    val data: List<Comment>
)

data class CommentPostResponse(
    val data: Boolean
)
