package com.vlog.app.data.users

/**
 * 注册请求
 */
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String
)
