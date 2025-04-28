package com.vlog.app.data.users

/**
 * 用户数据模型
 */
data class User(
    val id: String? = null,
    val createdAt: String? = null,
    val name: String? = null,
    val nickName: String? = null,
    val description: String? = null,
    val avatar: String? = null,
    val accessToken: String? = null
)