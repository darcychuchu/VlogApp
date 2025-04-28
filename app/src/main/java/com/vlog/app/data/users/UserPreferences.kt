package com.vlog.app.data.users

import android.content.Context
import android.content.SharedPreferences

/**
 * 用户偏好设置
 * 用于存储用户登录状态和信息
 */
class UserPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE
    )

    /**
     * 保存用户信息
     */
    fun saveUser(user: User) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_NICKNAME, user.nickName)
            putString(KEY_USER_AVATAR, user.avatar)
            putString(KEY_USER_DESCRIPTION, user.description)
            putString(KEY_ACCESS_TOKEN, user.accessToken)
            putString(KEY_CREATED_AT, user.createdAt)
            putBoolean(KEY_IS_LOGGED_IN, true)
        }.apply()
    }

    /**
     * 获取当前登录的用户
     */
    fun getUser(): User? {
        val isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        return User(
            id = sharedPreferences.getString(KEY_USER_ID, null),
            name = sharedPreferences.getString(KEY_USER_NAME, null),
            nickName = sharedPreferences.getString(KEY_USER_NICKNAME, null),
            avatar = sharedPreferences.getString(KEY_USER_AVATAR, null),
            description = sharedPreferences.getString(KEY_USER_DESCRIPTION, null),
            accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null),
            createdAt = sharedPreferences.getString(KEY_CREATED_AT, null)
        )
    }

    /**
     * 检查用户是否已登录
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * 清除用户信息（登出）
     */
    fun clearUser() {
        sharedPreferences.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_NICKNAME)
            remove(KEY_USER_AVATAR)
            remove(KEY_USER_DESCRIPTION)
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_CREATED_AT)
            putBoolean(KEY_IS_LOGGED_IN, false)
        }.apply()
    }

    companion object {
        private const val PREF_NAME = "user_preferences"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_NICKNAME = "user_nickname"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_USER_DESCRIPTION = "user_description"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_CREATED_AT = "created_at"

        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferences(context)
                INSTANCE = instance
                instance
            }
        }
    }
}