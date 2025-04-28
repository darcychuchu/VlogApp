package com.vlog.app.data

data class ApiResponse<T>(
    val code: Int = 0,
    val message: String? = null,
    val data: T
) {
    override fun toString(): String {
        return "ApiResponse(code=$code, message=$message, data=$data)"
    }
}